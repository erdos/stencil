(ns stencil.service.core
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [stencil.api :as api]
            [clojure.java.io :refer [file]]
            [ring.middleware.json :refer [wrap-json-body]]))

(defn get-http-port []
  (Integer/parseInt (System/getenv "STENCIL_HTTP_PORT")))

(defn get-template-dir []
  (let [dir (file (System/getenv "STENCIL_TEMPLATE_DIR"))]
    (if-not (.exists dir)
      (throw (ex-info "Template directory does not exist!" {:status 500}))
      dir)))

(def -prepared (atom {}))

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

(defn -app [request]
  (wrap-err
   (case (:request-method request)
     :post
     (if-let [prepared (get-template (:uri request))]
       (let [rendered (api/render! prepared (:body request) :output :input-stream)]
         {:status 200
          :body rendered
          :headers {"content-type" "application/octet-stream"}}))
     ;; otherwise:
     (throw (ex-info "Method Not Allowed" {:status 405})))))

(def app (wrap-json-body -app {:keywords? false}))

(defn -main [& args]
  (let [http-port    (get-http-port)
        template-dir (get-template-dir)
        server (run-server app {:port http-port})]
    (while true (read-line))))
