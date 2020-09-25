(ns stencil.functions
  "Function definitions"
  (:require [stencil.types :refer :all]))

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
