(ns stencil.eval
  "converts Normalized Control AST -> Evaled token seq"
  (:require [stencil.log :as log]
            [stencil.infix :refer [eval-rpn]]
            [stencil.types :refer [control?]]
            [stencil.tokenizer :as tokenizer]
            [stencil.tree-postprocess :as tree-postprocess]))

(set! *warn-on-reflection* true)

(defmulti eval-step (fn [function data item] (:cmd item)))

(defmethod eval-step :default [_ _ item] [item])

(defn normal-control-ast->evaled-seq [data function items]
  (assert (map? data))
  (assert (ifn? function))
  (assert (or (nil? items) (sequential? items)))
  (eduction (mapcat (partial eval-step function data)) items))

(defmethod eval-step :if [function data item]
  (let [condition (eval-rpn data function (:condition item))]
    (log/trace "Condition {} evaluated to {}" (:condition item) condition)
    (->> (if condition (:then item) (:else item))
         (normal-control-ast->evaled-seq data function))))

(defmethod eval-step :echo [function data item]
  (let [value (eval-rpn data function (:expression item))]
    (log/trace "Echoing {} as {}" (:expression item) value)
    [{:text (if (control? value) value (str value))}]))

(defmethod eval-step :for [function data item]
  (let [items (seq (eval-rpn data function (:expression item)))]
    (log/trace "Loop on {} will repeat {} times" (:expression item) (count items))
    (if (seq items)
      (let [datamapper (fn [val key] (assoc data (name (:variable item)) val
                                                 (name (:index-var item)) key))
            datas  (if (or (instance? java.util.Map items) (map? items))
                      (map datamapper (vals items) (keys items))
                      (map datamapper items (range)))
            bodies (cons (:body-run-once item) (repeat (:body-run-next item)))]
        (mapcat (fn [data body] (normal-control-ast->evaled-seq data function body)) datas bodies))
      (:body-run-none item))))

(defn eval-executable [part data functions]
  (->> (:executable part)
       (#(doto % assert))
       (normal-control-ast->evaled-seq data functions)
       (tokenizer/tokens-seq->document)
       (tree-postprocess/postprocess)))
