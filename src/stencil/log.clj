(ns stencil.log)

(def get-logger (memoize (fn [^String name] (.getLogger (org.slf4j.LoggerFactory/getILoggerFactory) name))))

(def logger [] (get-logger (.toString (.getName *ns*))))

(defmacro ^:private def-log-level [level]
  (assert (symbol? level))
  ;; TODO: perhaps make it a macro!
  `(defn ~level [msg# & args#]
     (. (get-logger (.toString (.getName *ns*)))
        ~level msg# args#)))

(def-log-level trace)
(def-log-level debug)
(def-log-level info)
(def-log-level warn)
(def-log-level error)
(def-log-level fatal)
