(ns stencil.functions
  "Function definitions"
  (:require [stencil.types :refer :all]
            [stencil.util :refer [fail]]))

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

;; finds first nonempy argument
(defmethod call-fn "coalesce" [_ & args-seq]
  (some not-empty args-seq))

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
