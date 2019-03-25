(ns stencil.eval
  "converts Normalized Control AST -> Evaled token seq"
  (:import [stencil.types FragmentInvoke])
  (:require [stencil.infix :refer [eval-rpn]]
            [stencil.types :refer [control?]]
            [stencil.util :refer [trace]]))

(set! *warn-on-reflection* true)

(defmulti ^:private eval-step (fn [function data item]
                                (or (:cmd item)
                                    (when (instance? FragmentInvoke item) :fragment)
                                    (when (map? item) [:tag (:tag item)])
                                    (type item))))

(defmethod eval-step :default [_ _ item] [item])

(defmethod eval-step :if [function data item]
  (let [condition (eval-rpn data function (:condition item))]
    (trace "Condition %s evaluated to %s" (:condition item) condition)
    (if condition
      (mapcat (partial eval-step function data) (:then item))
      (mapcat (partial eval-step function data) (:else item)))))

(defmethod eval-step :echo [function data item]
  (let [value (eval-rpn data function (:expression item))]
    (trace "Echoing %s as %s" (:expression item) value)
    [{:text (if (control? value) value (str value))}]))

(defmethod eval-step :for [function data item]
  (let [items (seq (eval-rpn data function (:expression item)))]
    (trace "Loop on %s will repeat %s times" (:expression item) (count items))
    (if (seq items)
      (let [datas  (map #(assoc data (name (:variable item)) %) items)
            bodies (cons (:body-run-once item) (repeat (:body-run-next item)))]
        (mapcat (fn [data body] (mapcat (partial eval-step function data) body)) datas bodies))
      (:body-run-none item))))

(defmethod eval-step :fragment [f data item]
  [(assoc item :data data)])

; (assoc (stencil.types/->FragmentInvoke 1 2) :data 44)

#_ ; azert nem igy csinaljuk, mert nehez osszenyitni a Paragraph es Run objektumokt utana
(defmethod eval-step :cmd/include FragmentInvoke [f data item]
  (let [frag-name (:name item)
        inserted (model/insert-fragment! frag-name data)
        steps (:fragment/steps inserted)]
    (assert (seq steps))
    (doall (mapcat (partial eval-step f data) steps))))

(defn normal-control-ast->evaled-seq [data function items]
  (assert (map? data))
  (assert (ifn? function))
  (assert (or (nil? items) (sequential? items)))
  (mapcat (partial eval-step function data) items))

;; TODO: creating image files for qr code or barcode should take place here
