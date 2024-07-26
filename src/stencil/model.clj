(ns stencil.model
  "Handling the meta-model of OOXML documents.
   See: http://officeopenxml.com/anatomyofOOXML.php
  "
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io :refer [file]]
            [stencil.eval :as eval]
            [stencil.infix :refer [eval-rpn]]
            [stencil.merger :as merger]
            [stencil.util :refer [unlazy-tree]]
            [stencil.model.common :refer [->xml-writer resource-copier]]
            [stencil.ooxml :as ooxml]
            [stencil.model [numbering :as numbering] [relations :as relations]
             [style :as style] [content-types :as content-types] [fragments :as fragments]]
            [stencil.cleanup :as cleanup]
            [stencil.fs :as fs]))

(set! *warn-on-reflection* true)

(defn ->exec [xml-streamable]
  (with-open [stream (io/input-stream xml-streamable)]
    (-> (merger/parse-to-tokens-seq stream)
        (cleanup/process)
        (select-keys [:variables :dynamic? :executable :fragments]))))

(defn- ->submodel [dir f]
  {::path       (fs/unix-path (fs/unroll f))
   :source-file (file dir f)
   :executable  (->exec (file dir f))
   :relations   (relations/->rels dir f)})

(defn- assoc-slide-layouts-notes [main-document dir]
  (->> (for [hf     (:headers+footers main-document)
             :when  (:relations hf)
             target (relations/targets-by-type (:relations hf)
                                               #{relations/rel-type-slide-layout relations/rel-type-notes-slide})]
         (->submodel dir (file (fs/parent-file (file (::path hf))) target)))
       (doall)
       (assoc main-document ::slide-layouts)))

(defn load-template-model [dir, options-map]
  (assert (fs/exists? dir))
  (assert (fs/directory? dir))
  (assert (map? options-map))
  (let [main-rels          (relations/->rels dir nil)
        [main-document]    (relations/targets-by-type main-rels #{relations/rel-type-main})
        main-document-rels (relations/->rels dir main-document)
        ->exec (binding [merger/*only-includes* (boolean (:only-includes options-map))]
                 (bound-fn* ->exec))]
    {:content-types (content-types/parse-content-types dir)
     :source-folder dir
     :relations     main-rels
     :main          (-> {::path       main-document
                         :source-file (file dir main-document)
                         :executable  (->exec (file dir main-document))
                         :relations   main-document-rels
                         :headers+footers (doall
                                           (for [t (relations/targets-by-type main-document-rels relations/extra-relations)]
                                             (->submodel dir (fs/unroll (file (fs/parent-file (file main-document)) t)))))}
                        (assoc-slide-layouts-notes dir)
                        (style/assoc-style dir)
                        (numbering/assoc-numbering dir))}))


(defn load-fragment-model [dir options-map]
  (-> (load-template-model dir options-map)
      ;; Headers and footers are not used in fragments.
      (update :main dissoc :headers+footers)))


(defn- eval-model-part-exec [part data functions]
  (assert (:executable part))
  (assert (:dynamic? part))
  (let [[result fragments] (fragments/with-sub-fragments (eval/eval-executable part data functions))]
    {:xml    result
     :fragment-names fragments
     :writer (->xml-writer result)}))


(defn- eval-model-part [part data functions]
  (assert (:executable part))
  (assert (::path part))
  (if (:dynamic? (:executable part))
    (eval-model-part-exec (:executable part) data functions)
    {:writer (resource-copier part)
     :xml-delay (delay
                  (with-open [reader (io/input-stream (:source-file part))]
                    (unlazy-tree (xml/parse reader))))}))


(defn- eval-template-model [template-model data functions fragments]
  (assert (:main template-model) "Should be a result of load-template-model call!")
  (assert (some? fragments))
  (fragments/with-fragments fragments
    (content-types/with-content-types
      (style/with-styles-context template-model
        (numbering/with-numbering-context template-model
          (let [evaluate  (fn [m]
                            (relations/with-extra-files-context
                              (let [result         (eval-model-part m data functions)
                                    fragment-names (set (:fragment-names result))]
                                (-> m
                                    (relations/model-assoc-extra-files fragment-names)
                                    (assoc :result result)))))]
            (-> template-model
                (update-in [:main :headers+footers] (partial mapv evaluate))
                (update-in [:main ::slide-layouts] (partial mapv evaluate))
                (update :main evaluate))))))))

(defn- model-seq [model]
  (let [model-keys [:relations :headers+footers :main :style :content-types :fragments ::numbering :result ::slide-layouts]]
    (tree-seq map? (fn [node] (flatten (keep node model-keys))) model)))


;; returns a map where key is path and value is writer fn.
(defn- evaled-template-model->writers-map [evaled-template-model]
  (as-> (sorted-map) result

    ;; relations files that are created on-the-fly
    (into result
          (for [m (model-seq evaled-template-model)
                :when (::path m)
                :when (:parsed (:relations m))
                :when (not (:source-file (:relations m)))]
            [(::path (:relations m)) (relations/writer (:parsed (:relations m)))]))

    ;; create writer for every item where ::path is specified
    (into result
          (for [m (model-seq evaled-template-model)
                :when (::path m)
                :when (not= "External" (::mode m))
                :when (not (contains? result (::path m)))]
            [(::path m) (or (:writer (:result m)) (resource-copier m))]))

    ;; find all items in all relations
    (into result
          (for [m (model-seq evaled-template-model)
                :when (:relations m) ;:when (::path m)
                :let [src-parent  (delay (file (or (:source-folder m)
                                                   (fs/parent-file (file (:source-file m))))))
                      path-parent (some-> m ::path file fs/parent-file)]
                relation (vals (:parsed (:relations m)))
                :when (not= "External" (::mode relation))
                :let [path (fs/unix-path (fs/unroll (file path-parent (::target relation))))]
                :when (or (:writer relation) (not (contains? result path)))
                :let [src (or (:source-file relation) (file @src-parent (::target relation)))]]
            [path (or (:writer relation)
                      (resource-copier {::path path :source-file src}))]))))


(defn template-model->writers-map
  "Evaluates a prepared template and returns a {path writer-fn} map that can be used to write the zip stream."
  [template data function fragments]
  (assert (map? data))
  (-> template
      (eval-template-model data function fragments)
      (evaled-template-model->writers-map)))


(defn- extract-body-parts [xml-tree]
  (assert (:tag xml-tree))
  (doall
   (for [body (:content xml-tree)
         :when (= "body" (name (:tag body)))
         elem (:content body)
         :when (not= "sectPr" (name (:tag elem)))]
     elem)))


;; recursively going over xml tree, rename values in attributes specified in mappers.
(defn- xml-map-attrs [attr-mappers xml-tree]
  (if (map? xml-tree)
    ;; TODO: we could speed this up!
    (if-let [f (attr-mappers (:tag xml-tree))]
      (update-in xml-tree [:attrs ooxml/val] f)
      (assoc xml-tree :content (mapv (partial xml-map-attrs attr-mappers) (:content xml-tree))))
    xml-tree))

; And therefore:
; (defn- map-rename-relation-ids [item id-rename]
;   (xml-map-attrs {ooxml/r-embed id-rename ooxml/r-id id-rename} item))


(defmethod eval/eval-step :cmd/include [function local-data-map step]
  (assert (map? local-data-map))
  (let [frag-name        (eval-rpn local-data-map function (:name step))
        fragment-model   (fragments/use-fragment frag-name)
        style-ids-rename (-> fragment-model :main :style :parsed (doto assert) (style/insert-styles!))

        relation-ids-rename (relations/ids-rename fragment-model frag-name)
        relation-rename-map (into {} (map (juxt :old-id :new-id)) relation-ids-rename)

        ;; evaluate
        evaled (eval-template-model fragment-model local-data-map function {})

        ;; write back
        get-xml      (fn [x] (or (:xml x) @(:xml-delay x)))
        evaled-parts (->> evaled :main :result
                          (get-xml)
                          (extract-body-parts)
                          (map (partial relations/xml-rename-relation-ids relation-rename-map))
                          (map (partial xml-map-attrs
                                        {ooxml/attr-numId
                                         (partial numbering/copy-numbering fragment-model (atom {}))}))
                          (map (partial style/xml-rename-style-ids style-ids-rename))
                          (doall))]
    (run! relations/add-extra-file! relation-ids-rename)
    [{:text (fragments/->FragmentInvoke {:frag-evaled-parts evaled-parts})}]))
