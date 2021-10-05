(ns stencil.log)

(def get-logger
  (memoize (fn [^String name] (.getLogger (org.slf4j.LoggerFactory/getILoggerFactory) name))))

(defn ^org.slf4j.Logger logger []
  (get-logger (.toString (.getName *ns*))))

(defmacro ^:private def-log-level [level]
  `(defmacro ~level
     ([^String msg#]
      (assert (string? msg#))
      (assert (not (.contains msg# "{}")))
      (list '. '(stencil.log/logger) '~level msg#))
     ([msg# arg#]
      (assert (string? msg#))
      (list '. '(stencil.log/logger) '~level msg# arg#))
     ([msg# arg0# ~'& args#]
      (assert (string? msg#))
      (->> (list* 'list arg0# args#)
           (list 'to-array)
           (list '. '(stencil.log/logger) '~level msg#)))))

(declare trace debug info warn error fatal)

(def-log-level trace)
(def-log-level debug)
(def-log-level info)
(def-log-level warn)
(def-log-level error)
