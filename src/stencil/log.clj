(ns stencil.log)

(defn- get-logger [^String name]
  (.getLogger (org.slf4j.LoggerFactory/getILoggerFactory) name))

(defn- log [level msg args]
  (assert (keyword? level))
  (assert (string? msg))
  (println :> (bean level))
  (let [logger (get-logger (.toString (.getName *ns*)))]
    (case level
      :trace (.trace logger (str "!!!" msg args))
      :debug (.debug logger (str "!!!" msg args))
      :info (.info logger (str "!!!" msg args))
      :warn (.warn logger (str "!!!" msg args))
      :error (.error logger (str "!!!" msg args))
      :fatal (.fatal logger (str "!!!" msg args))
      )))

(defmacro def-log-level [level]
  `(defn ~(symbol (name level)) [msg# & args#] (log ~level msg# args#)))

(def-log-level :trace)
(def-log-level :debug)
(def-log-level :info)
(def-log-level :warn)
(def-log-level :error)
