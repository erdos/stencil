(ns stencil.service
  (:gen-class)
  (:import [java.io File])
  (:require [org.httpkit.server :refer [run-server]]
            [stencil.api :as api]
            [stencil.functions]
            [stencil.log :as log]
            [stencil.slf4j :as slf4j]
            [clojure.data :refer [diff]]
            [clojure.java.io :as io :refer [file]]
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

(defn eval-js-file []
  (let [f (file (get-template-dir) "stencil.js")]
    (when (.exists f)
      (log/info "Evaluating stencil.js file")
      (let [manager (new javax.script.ScriptEngineManager)
            engine (.getEngineByName manager "rhino")
            invocable ^javax.script.Invocable engine
            context (.getContext engine)]
        (with-open [r (io/reader f)]
          (.eval engine r context))
        (doseq [[k] (.getBindings context javax.script.ScriptContext/ENGINE_SCOPE)]
          (log/info "Defining function created in stencil.js file: {}" k)
          (defmethod stencil.functions/call-fn k [k & args]
            (.invokeFunction invocable k (into-array Object args))))))))

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
           (if-let [status (:status (ex-data e))]
             (do (log/error "Render error={}" (.getMessage e))
                 {:status status
                  :body (str "ERROR: " (.getMessage e))})
             (do (log/error "Error" e)
                 (throw e))))
         (catch Exception e
            {:status 400
             :body (pr-str e)}
         ))))

(defn- wrap-log [handler]
  (fn [req]
    (slf4j/with-mdc ["log-level" (get-in req [:headers "x-stencil-log"] "info")
                     "corr-id"   (or (get-in req [:headers "x-stencil-corr-id"])
                                     (subs (str (java.util.UUID/randomUUID)) 0 8))]
      (let [response (handler req)]
        (when (not= 200 (:status response))
          (log/error "Response status was {}" (:status response)))
        response))))

(defn -app [request]
  (cond
    (and (= :get (:request-method request)) (= "/" (:uri request)))
    {:status 200
     :body "I am alive."}

    (= :post (:request-method request) :post)
    (let [prepared (get-template (:uri request))
          rendered (api/render! prepared (:body request) :output :input-stream)]
      (log/info "Successfully rendered template {}" (:uri request))
      (flush)
      {:status 200
       :body rendered
       :headers {"content-type" "application/octet-stream"}})

    :else
    (throw (ex-info "Method Not Allowed" {:status 405}))))

(def app
  (-> -app
      (wrap-json-body {:keywords? false})
      (wrap-err)
      (wrap-log)))

(defn -main [& args]
  (log/info "Starting Stencil Service {}" api/version)
  (eval-js-file)
  (let [http-port    (get-http-port)
        template-dir ^File (get-template-dir)
        server (run-server app {:port http-port})]
    (log/info "Started listening on {} serving {}" http-port (str template-dir))
    (log/info "Available template files:")
    (doseq [^File line (file-seq template-dir)
            :when (.isFile line)]
      (log/info "- {}" (.relativize (.toPath template-dir) (.toPath line))))))
