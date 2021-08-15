(ns stencil.slf4j)

(def log-levels-upto
  (let [levels [:trace :debug :info :warn :error :fatal]]
    (into {} (map-indexed (fn [idx level] [(name level) (set (drop idx levels))]) levels))))

(def ^:dynamic *active-log-levels* (log-levels-upto "debug"))
(def ^:dynamic *corr-id* "SYSTEM")

(deftype StencilLoggerFactory []
  org.slf4j.ILoggerFactory
  (getLogger [_ caller-name]
    (proxy [org.slf4j.helpers.AbstractLogger] []
      (isTraceEnabled [] (contains? *active-log-levels* :trace))
      (isDebugEnabled [] (contains? *active-log-levels* :debug))
      (isInfoEnabled [] (contains? *active-log-levels* :info))
      (isWarnEnabled [] (contains? *active-log-levels* :warn))
      (isErrorEnabled [] (contains? *active-log-levels* :error))
      (isFatalEnabled [] (contains? *active-log-levels* :fatal))
      (getFullyQualifiedCallerName [] caller-name)
      (handleNormalizedLoggingCall [level marker msg args throwable]
        (println (str (java.time.LocalDateTime/now)) (str level) caller-name *corr-id* ":" args throwable)))))

(deftype SLF4JServiceProvider []
  org.slf4j.spi.SLF4JServiceProvider
  (getLoggerFactory [_] (new StencilLoggerFactory))
  (getMarkerFactory [_] nil)
  (getMDCAdapter [_] nil)
  (getRequesteApiVersion [_] "1.8") ;; TODO
  (initialize [_]))
