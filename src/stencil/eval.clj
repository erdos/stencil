(ns stencil.eval
  "converts Normalized Control AST -> Evaled token seq"
  (:require [stencil.log :as log]
            [stencil.infix :refer [eval-rpn]]
            [stencil.types :refer [control?]]
            [stencil.tokenizer :as tokenizer]
            [stencil.util :refer [eval-exception]]
            [stencil.tree-postprocess :as tree-postprocess]
            [stencil.ooxml :as ooxml]))

(set! *warn-on-reflection* true)

(defmulti eval-step (fn [function data trace item] (or (:cmd item) (:open+close item))))

(defmethod eval-step :default [_ _ _ item] [item])

(defn normal-control-ast->evaled-seq [data function trace items]
  (assert (map? data))
  (assert (ifn? function))
  (assert (or (nil? items) (sequential? items)))
  (eduction (mapcat (partial eval-step function data trace)) items))

(defn- eval-rpn* [data function expr raw-expr]
  (try (eval-rpn data function expr)
       (catch Exception e
              (throw (eval-exception (str "Error evaluating expression: " raw-expr) e)))))

(defmethod eval-step :if [function data trace item]
  (let [condition (eval-rpn* data function (:condition item) (:raw item))]
    (log/trace "Condition {} evaluated to {}" (:condition item) condition)
    (->> (if condition (:branch/then item) (:branch/else item))
         (normal-control-ast->evaled-seq data function trace))))

(defmethod eval-step :cmd/echo [function data _ item]
  (let [value (eval-rpn* data function (:expression item) (:raw item))]
    (log/trace "Echoing {} as {}" (:expression item) value)
    [{:text (if (control? value) value (str value))}]))

(defmethod eval-step :for [function data trace item]
  (let [items (eval-rpn* data function (:expression item) (:raw item))]
    (log/trace "Loop on {} will repeat {} times" (:expression item) (count items))
    (if (not-empty items)
      (let [index-var-name (name (:index-var item))
            loop-var-name  (name (:variable item))
            datamapper     (fn [key val] (assoc data, index-var-name key, loop-var-name val))
            datas          (if (or (instance? java.util.Map items) (map? items))
                             (map datamapper (keys items) (vals items))
                             (map-indexed datamapper items))
            bodies (cons (:branch/body-run-once item) (repeat (:branch/body-run-next item)))
            traces (for [i (range)] (cons i trace))]
        (mapcat (fn [data body trace] (normal-control-ast->evaled-seq data function trace body)) datas bodies traces))
      (:branch/body-run-none item))))

(defmethod eval-step ooxml/attr-numId [_ _ trace item]
  [(assoc-in item [:attrs ::trace] trace)])

(defn eval-executable [part data functions]
  (->> (:executable part)
       (#(doto % assert))
       (normal-control-ast->evaled-seq data functions ())
       (tokenizer/tokens-seq->document)
       (tree-postprocess/postprocess)))
