
(ns stencil.log)

(def get-logger (memoize (fn [^String name] (.getLogger (org.slf4j.LoggerFactory/getILoggerFactory) name))))

(defn ^org.slf4j.Logger logger [] (get-logger (.toString (.getName *ns*))))

(defmacro ^:private def-log-level [level]
  `(defmacro ~level
     ([msg# arg#]
      (assert (string? msg#))
      (list '. '(stencil.log/logger) '~level msg# arg#))
     ([msg# arg0# ~'& args#]
      (assert (string? msg#))
      (list '. '(stencil.log/logger) '~level msg# (list 'to-array (list* 'list arg0# args#))))))

(declare trace debug info warn error fatal)

(def-log-level trace)
(def-log-level debug)
(def-log-level info)
(def-log-level warn)
(def-log-level error)
(def-log-level fatal)
