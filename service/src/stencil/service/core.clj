(ns stencil.service.core
  (:gen-class)
  (:import [java.io File])
  (:require [org.httpkit.server :refer [run-server]]
            [stencil.api :as api]
            [clojure.data :refer [diff]]
            [clojure.java.io :refer [file]]
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

(defn get-template [^String template-name]
  (let [template-name (.substring (str template-name) 1) ;; so they dont start with /
        parent (get-template-dir)
        template (file parent template-name)]
    (if (.exists template)
      (prepared template)
      (throw (ex-info "Template file does not exist!" {:status 404})))))

(defmacro wrap-err [& bodies]
  `(try ~@bodies
        (catch clojure.lang.ExceptionInfo e#
          (if-let [status# (:status (ex-data e#))]
            {:status status#, :body (.getMessage e#)}
            (throw e#)))))

(defn- wrap-log [handler]
  (fn [request]
    (let [log-level (get-in handler [:headers "x-stencil-log"] "INFO")]
      (when-not (#{"INFO" "DEBUG" "TRACE" "WARN" "ERROR"} log-level)
        (throw (ex-info "Unexpected log level header value!" {:status 400})))
      (handler request)
      )))

(defn -app [request]
  (->
   (case (:request-method request)
     :post
     (if-let [prepared (get-template (:uri request))]
       (let [rendered (api/render! prepared (:body request) :output :input-stream)]
         {:status 200
          :body rendered
          :headers {"content-type" "application/octet-stream"}}))
     ;; otherwise:
     (throw (ex-info "Method Not Allowed" {:status 405})))
   (wrap-err)
   (wrap-log)))

(def app (wrap-json-body -app {:keywords? false}))

(defn -main [& args]
  (let [http-port    (get-http-port)
        template-dir ^File (get-template-dir)
        server (run-server (wrapp-log app) {:port http-port})]
    (println "Started listening on" http-port "serving" (str template-dir))
    (println "Available template files: ")
    (doseq [^File line (tree-seq #(.isDirectory ^File %) (comp next file-seq) template-dir)
            :when (.isFile line)]
      (println (str (.relativize (.toPath template-dir) (.toPath line)))))
    (while true (read-line))))
