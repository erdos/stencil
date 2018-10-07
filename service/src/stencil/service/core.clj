(ns stencil.service.core
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [stencil.api :as api]
            [clojure.java.io :refer [file]]))


(defn get-http-port []
  (Integer/parseInt (System/getenv "STENCIL_HTTP_PORT")))


(defn get-template-dir []
  (let [dir (file (System/getenv "STENCIL_TEMPLATE_DIR"))]
    (when-not (.exists dir)
      (throw (ex-info "Template directory does not exist!" {:dir dir})))
    ;; todo: check if dir is empty?

    dir))


(def prepared (memoize api/prepare))


(defn get-template [^String template-name]
  (let [template-name (.substring (str template-name) 1) ;; so they dont start with /
        parent (get-template-dir)
        template (file parent template-name)]
    (when (.exists template)
      (prepared template))))


(defn app [request]
  (case (:request-method request)
    :post
    (if-let [prepared (get-template (:uri request))]
      (let [rendered (api/render! prepared {} :output :input-stream)]
        {:status 200
         :body rendered
         ;; TODO: content type headers!
         })
      {:status 404 :body "Template Not Found!"})
    {:status 405 :body "Method Not Allowed"}))


(defn -main [& args]
  (let [http-port    (get-http-port)
        template-dir (get-template-dir)
        server (run-server app {:port http-port})]
    (while true (read-line))))
