(ns stencil.service.core)


(defn app [request]
  ;; TODO: get route from uri
  ;;
  )

(defn get-http-port []
  (Integer/parseInt (System/getEnv "STENCIL_HTTP_PORT")))

(defn get-template-dir []
  (let [dir (clojure.java.io/file (System/getEnv "STENCIL_TEMPLATE_DIR"))]
    (when-not (.exists dir)
      (throw (ex-info "Template directory does not exist!" {:dir dir})))
    ;; todo: check if dir is empty?

    dir))

;; template name to prepared template.
(defn get-template [template-name])

(defn -main [& args]
  (let [http-port    (get-http-port)
        template-dir (get-template-dir)]
    ;; TODO: start http server

    ))
