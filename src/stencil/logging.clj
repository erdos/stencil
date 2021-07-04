(ns stencil.logging)

(def print-trace? false)

(def ^:dynamic *log-levels* #{:info :warn :error :fatal})

(defmacro with-log-level [levels & bodies]
  `(binding [*log-levels* ~levels] ~@bodies))

(defmacro trace [msg & details]
  (assert (string? msg) "Log message must be a string")
  `(when (:trace *log-levels*)
     (println (format ~msg ~@(for [d details] `(pr-str ~d))))))

(defmacro debug [msg & details]
  (assert (string? msg) "Log message must be a string")
  `(when (:trace *log-levels*)
     (println (format ~msg ~@(for [d details] `(pr-str ~d))))))
