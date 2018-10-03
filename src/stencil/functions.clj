(ns stencil.functions
  "Function definitions"
  (:require [stencil.types :refer :all]))

(set! *warn-on-reflection* true)

(defmulti call-fn (fn [function-name & args-seq] function-name))

(defmethod call-fn "range"
  ([_ x] (range x))
  ([_ x y] (range x y))
  ([_ x y z] (range x y z)))

;; finds first nonempy argument
(defmethod call-fn "coalesce" [_ & args-seq]
  (some #(when-not (empty? %) %) args-seq))

(defmethod call-fn "length" [_ items] (count items))

(defmethod call-fn "hideColumn" [_ & args]
  (case (first args)
    ("cut") (->HideTableColumnMarker :cut)
    ("resize-last" "resizeLast" "resize_last") (->HideTableColumnMarker :resize-last)
    ("rational")                 (->HideTableColumnMarker :rational)
    ;; default
    (->HideTableColumnMarker)))

(defmethod call-fn "hideRow" [_] (->HideTableRowMarker))
