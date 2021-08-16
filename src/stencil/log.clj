(ns stencil.log)

(def get-logger (memoize (fn [^String name] (.getLogger (org.slf4j.LoggerFactory/getILoggerFactory) name))))

(defn logger [] (get-logger (.toString (.getName *ns*))))

(defmacro ^:private def-log-level [level]
  (assert (symbol? level))
  ;; TODO: perhaps make it a macro!
  `(defn ~level
     ([^String msg#] (. (logger) ~level msg#))
     ([^String msg# arg#] (. (logger) ~level msg# arg#))
     ([^String msg# arg0# & args#]
      (. (logger) ~level msg# (to-array (cons arg0# args#))))))

(def-log-level trace)
(def-log-level debug)
(def-log-level info)
(def-log-level warn)
(def-log-level error)
(def-log-level fatal)
