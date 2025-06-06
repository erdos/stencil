(ns stencil.eval
  "converts Normalized Control AST -> Evaled token seq"
  (:require [stencil.log :as log]
            [stencil.infix :refer [eval-rpn]]
            [stencil.tokenizer :as tokenizer]
            [stencil.util :refer [eval-exception]]
            [stencil.tree-postprocess :as tree-postprocess]))

(set! *warn-on-reflection* true)

(defmulti eval-step (fn [_function _data item] (:cmd item)))

(defmethod eval-step :default [_ _ item] [item])

(defn normal-control-ast->evaled-seq [data function items]
  (assert (map? data))
  (assert (ifn? function))
  (assert (or (nil? items) (sequential? items)))
  (eduction (mapcat (partial eval-step function data)) items))

(defn- eval-rpn* [data function expr raw-expr]
  (try (eval-rpn data function expr)
       (catch Exception e
              (throw (eval-exception (str "Error evaluating expression: " raw-expr) e)))))

(defmethod eval-step :cmd/if [function data item]
  (let [condition (eval-rpn* data function (:condition item) (:raw item))]
    (log/trace "Condition {} evaluated to {}" (:condition item) condition)
    (->> (if condition (:branch/then item) (:branch/else item))
         (normal-control-ast->evaled-seq data function))))

(defmethod eval-step :cmd/echo [function data item]
  (let [value (eval-rpn* data function (:expression item) (:raw item))]
    (log/trace "Echoing {} as {}" (:expression item) value)
    [{:text (if (instance? clojure.lang.IRecord value) value (str value))}]))

(defmethod eval-step :cmd/for [function data item]
  (let [items (eval-rpn* data function (:expression item) (:raw item))]
    (log/trace "Loop on {} will repeat {} times" (:expression item) (count items))
    (if (not-empty items)
      (let [index-var-name (name (:index-var item))
            loop-var-name  (name (:variable item))
            datamapper     (fn [key val] (assoc data, index-var-name key, loop-var-name val))
            datas          (if (or (instance? java.util.Map items) (map? items))
                             (map datamapper (keys items) (vals items))
                             (map-indexed datamapper items))
            bodies (cons (:branch/body-run-once item) (repeat (:branch/body-run-next item)))]
        (mapcat (fn [data body] (normal-control-ast->evaled-seq data function body)) datas bodies))
      (:branch/body-run-none item))))

(defn eval-executable [part data functions]
  (->> (:executable part)
       (#(doto % assert))
       (normal-control-ast->evaled-seq data functions)
       (tokenizer/tokens-seq->document)
       (tree-postprocess/postprocess)))
