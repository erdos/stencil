(ns stencil.functions
  "Function definitions"
  (:require [stencil.types :refer :all]
            [stencil.util :refer [fail find-first]]))

(set! *warn-on-reflection* true)

(defmulti call-fn
  "Extend this multimethod to make additional functions available from the template files.
   The first argument is the lowercase function name which is used for dispatching the calls.
   The rest of the arguments are the function call parameters."
  (fn [function-name & args-seq] function-name))

(defmethod call-fn "range"
  ([_ x] (range x))
  ([_ x y] (range x y))
  ([_ x y z] (range x y z)))

(defmethod call-fn "integer" [_ n] (some-> n biginteger))
(defmethod call-fn "decimal" [_ f] (some-> f bigdec))

;; The format() function calls java.lang.String.format()
;; but it predicts the argumet types from the format string and
;; converts the argument values to the correct types to prevent runtime errors.
(let [fs-pattern #"%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])"
      get-types  (fn [pattern-str]
                   (second (reduce (fn [[max-idx types] [_ idx _ _ _ _ type]]
                                     (if idx
                                       [max-idx (assoc types (Long/valueOf idx) type)]
                                       [(inc max-idx) (assoc types max-idx type)]))
                                   [0 {}]
                                   (re-seq fs-pattern pattern-str))))
      cache (atom ())
      cache-size 32
      get-types (fn [p] (or (some (fn [[k v]] (when (= k p) v)) @cache)
                            (doto (get-types p)
                              (->> (swap! cache (fn [c t] (take cache-size (cons [p t] c))))))))]
  (defmethod call-fn "format" [_ pattern-str & args]
    (when-not (string? pattern-str)
      (fail "Format pattern must be a string!" {:pattern pattern-str}))
    (let [types (get-types pattern-str)]
      (->> args
           (map-indexed (fn [idx value]
                          (case (types idx)
                            ("c" "C")                     (some-> value char)
                            ("d" "o" "x" "X")             (some-> value biginteger)
                            ("e" "E" "f" "g" "G" "a" "A") (some-> value bigdec)
                            value)))
           (to-array)
           (String/format pattern-str)))))

;; finds first nonempy argument
(defmethod call-fn "coalesce" [_ & args-seq]
  (find-first (some-fn number? true? false? not-empty) args-seq))

(defmethod call-fn "length" [_ items] (count items))

(defmethod call-fn "contains" [_ item items]
  (boolean (some #{(str item)} (map str items))))

(defmethod call-fn "sum" [_ items]
  (reduce + items))

(defmethod call-fn "hideColumn" [_ & args]
  (case (first args)
    ("cut") (->HideTableColumnMarker :cut)
    ("resize-last" "resizeLast" "resize_last") (->HideTableColumnMarker :resize-last)
    ("resize-first" "resizeFirst resize_first") (->HideTableColumnMarker :resize-first)
    ("rational")                 (->HideTableColumnMarker :rational)
    ;; default
    (->HideTableColumnMarker)))

(defmethod call-fn "hideRow" [_] (->HideTableRowMarker))

(defn- lookup [column data]
  (second (or (find data column)
              (find data (keyword column)))))

(defmethod call-fn "map" [_ ^String column data]
  (when-not (string? column)
    (fail "First parameter of map() must be a string!" {}))
  (reduce (fn [elems p]
            (if (empty? p)
              (do (doseq [e elems :when (not (or (nil? e) (sequential? e)))]
                    (fail "Wrong data, expected sequence, got: " {:data e}))
                  (mapcat seq elems))
              (do (doseq [e elems :when (not (or (nil? e) (map? e)))]
                    (fail "Wrong data, expected map, got: " {:data e}))
                  (keep (partial lookup p) elems))))
          data
          (.split column "\\.")))
