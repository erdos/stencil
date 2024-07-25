(ns stencil.log)

(def ^org.slf4j.Logger get-logger
  (memoize (fn [^String name] (.getLogger (org.slf4j.LoggerFactory/getILoggerFactory) name))))

(defmacro ^:private def-log-level [level]
  `(defmacro ~level
     ([^String msg#]
      (assert (string? msg#))
      (assert (not (.contains msg# "{}")))
      (list '. (list 'stencil.log/get-logger (str *ns*)) '~level msg#))
     ([msg# arg#]
      (assert (string? msg#))
      (assert (>= 1 (count (re-seq #"\{\}" msg#))))
      (list '. (list 'stencil.log/get-logger (str *ns*)) '~level msg# arg#))
     ([msg# arg0# ~'& args#]
      (assert (string? msg#))
      (assert (< (count args#) (count (re-seq #"\{\}" msg#)) (+ 2 (count args#))))
      (->> (list* 'list arg0# args#)
           (list 'to-array)
           (list '. (list 'stencil.log/get-logger (str *ns*)) '~level msg#)))))

(def-log-level trace)
(def-log-level debug)
(def-log-level info)
(def-log-level warn)
(def-log-level error)

(ns-unmap *ns* 'def-log-level)