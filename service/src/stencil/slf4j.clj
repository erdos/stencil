(ns stencil.slf4j)

(set! *warn-on-reflection* true)

(defmacro with-mdc [bindings & bodies]
  `(try
     ~@(for [[k v] (partition 2 bindings)]
         (list 'org.slf4j.MDC/put k v))
     ~@bodies
     (finally (org.slf4j.MDC/clear))))

(def default-log-level (not-empty (System/getenv "STENCIL_LOG_LEVEL")))

(defn get-log-level []
  (or (org.slf4j.MDC/get "log-level") default-log-level "info"))

(defn get-corr-id []
  (or (org.slf4j.MDC/get "corr-id") "SYSTEM"))

(deftype StencilLoggerFactory []
  org.slf4j.ILoggerFactory
  (getLogger [_ caller-name]
    (proxy [org.slf4j.helpers.AbstractLogger] []
      (isTraceEnabled [] (contains? #{"trace"} (get-log-level)))
      (isDebugEnabled [] (contains? #{"trace" "debug"} (get-log-level)))
      (isInfoEnabled []  (contains? #{"trace" "debug" "info"} (get-log-level)) )
      (isWarnEnabled []  (contains? #{"trace" "debug" "info" "warn"} (get-log-level)))
      (isErrorEnabled [] (contains? #{"trace" "debug" "info" "warn" "error"} (get-log-level)))
      (isFatalEnabled [] true)
      (getFullyQualifiedCallerName [] caller-name)
      (handleNormalizedLoggingCall [level marker msg args throwable]
        (println (str (java.time.LocalDateTime/now))
                 (str level) caller-name (get-corr-id) ":" args throwable)))))

(def mdc-adapter (new org.slf4j.helpers.BasicMDCAdapter))
(def logger-factory (new StencilLoggerFactory))

(deftype SLF4JServiceProvider []
  org.slf4j.spi.SLF4JServiceProvider
  (getLoggerFactory [_] logger-factory)
  (getMarkerFactory [_] nil)
  (getMDCAdapter [_] mdc-adapter)
  (getRequesteApiVersion [_] "1.8") ;; TODO
  (initialize [_]))
