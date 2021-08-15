(ns stencil.service
  (:gen-class)
  (:import [java.io File]
           [org.slf4j MDC])
  (:require [org.httpkit.server :refer [run-server]]
            [stencil.api :as api]
            [stencil.log :as log]
            [stencil.slf4j :as slf4j]
            [clojure.data :refer [diff]]
            [clojure.java.io :refer [file]]
            [clojure.string :as s]
            [ring.middleware.json :refer [wrap-json-body]]))

(set! *warn-on-reflection* true)

(defn get-http-port []
  (try (Integer/parseInt (System/getenv "STENCIL_HTTP_PORT"))
       (catch NumberFormatException e
         (throw (ex-info "Missing STENCIL_HTTP_PORT property!" {})))))

(defn get-template-dir []
  (if-let [dir (some-> (System/getenv "STENCIL_TEMPLATE_DIR") (file))]
    (if-not (.exists dir)
      (throw (ex-info "Template directory does not exist!" {:status 500}))
      dir)
    (throw (ex-info "Missing STENCIL_TEMPLATE_DIR property!" {}))))

(def -prepared
  "Map of {file-name {timestamp prepared}}."
  (doto (atom {})
    (add-watch :cleanup
               (fn [_ _ before after]
                 (let [[old-templates _ _] (diff before after)]
                   (run! api/cleanup! (mapcat vals (vals old-templates))))))))

(defn prepared [template-name]
  (let [template-file (file template-name)
        last-modified (.lastModified template-file)]
    (or (get-in @-prepared [template-file last-modified])
       (let [p (api/prepare template-file)]
         (swap! -prepared assoc template-file {last-modified p})
         p))))

(defn get-template [template-name]
  (let [template-name (.substring (str template-name) 1) ;; so they dont start with /
        parent (get-template-dir)
        template (file parent template-name)]
    (if (.exists template)
      (prepared template)
      (throw (ex-info "Template file does not exist!" {:status 404})))))

(defn wrap-err [handler]
  (fn [request]
    (try (handler request)
         (catch clojure.lang.ExceptionInfo e
           (log/error "Error" e)
           (if-let [status (:status (ex-data e))]
             {:status status
              :body (str "ERROR: " (.getMessage e))}
             (throw e))))))

(defn- wrap-log [handler]
  (fn [req]
    (binding [slf4j/*active-log-levels* (slf4j/log-levels-upto (get-in req [:headers "x-stencil-log"] "info"))
              slf4j/*corr-id*           (or (get-in req [:headers "x-stencil-corr-id"])
                                            (subs (str (java.util.UUID/randomUUID)) 0 8))]
      (try (MDC/put "corr-id" (str slf4j/*corr-id*))
           (handler req)
           (finally (MDC/clear))))))

(defn -app [request]
  (cond
    (and (= :get (:request-method request)) (= "/" (:uri request)))
    {:status 200
     :body "I am alive."}

    (= :post (:request-method request) :post)
    (if-let [prepared (get-template (:uri request))]
      (let [rendered (api/render! prepared (:body request) :output :input-stream)]
        (log/info "Successfully rendered template" (:uri request))
        (flush)
        {:status 200
         :body rendered
         :headers {"content-type" "application/octet-stream"}}))

    :else
    (throw (ex-info "Method Not Allowed" {:status 405}))))

(def app
  (-> -app
      (wrap-json-body {:keywords? false})
      (wrap-log)
      (wrap-err)))

(defn -main [& args]
  (log/info "Starting")
  (log/debug "Starting")
  (let [http-port    (get-http-port)
        template-dir ^File (get-template-dir)
        server (run-server app {:port http-port})]
    (log/info "Started listening on" http-port "serving" (str template-dir))
    (log/info "Available template files: ")
    (doseq [^File line (file-seq template-dir)
            :when (.isFile line)]
      (log/info (str (.relativize (.toPath template-dir) (.toPath line)))))))
