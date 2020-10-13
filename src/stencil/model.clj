(ns stencil.model
  "Handling the meta-model of OOXML documents.
   See: http://officeopenxml.com/anatomyofOOXML.php
  "
  (:import [java.io File]
           [java.nio.file Files]
           [io.github.erdos.stencil.impl FileHelper])
  (:require [clojure.data.xml :as xml]
            [clojure.data.xml.pu-map :as pu]
            [clojure.java.io :as io :refer [file]]
            [clojure.walk :refer [postwalk]]
            [stencil.eval :as eval]
            [stencil.merger :as merger]
            [stencil.tree-postprocess :as tree-postprocess]
            [stencil.types :refer [->FragmentInvoke]]
            [stencil.util :refer :all]

            [stencil.model.relations :as relations]
            [stencil.model.common :refer :all]
            [stencil.model.style :as style
             :refer [expect-fragment-context! *current-styles*]]
            [stencil.cleanup :as cleanup]))

(set! *warn-on-reflection* true)

(def rel-type-main
  "Relationship type of main document in _rels/.rels file."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument")

(def rel-type-footer
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer")

(def rel-type-header
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/header")

(def rel-type-slide
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide")

;; all insertable fragments. map of id to frag def.
(def ^:private ^:dynamic *all-fragments* nil)

;; set of already inserted fragment ids.
(def ^:private ^:dynamic *inserted-fragments* nil)

;; list of extra relations to be added after evaluating document
(def ^:private ^:dynamic *extra-files* nil)


(defn- unix-path [^File f]
  (some-> f .toPath FileHelper/toUnixSeparatedString))


(defn- parse-content-types [^File cts]
  (assert (.exists cts))
  (assert (.isFile cts))
  (let [parsed (with-open [r (io/input-stream cts)] (xml/parse r))]
    {:source-file cts
     ::path       (.getName cts)}))


;; TODO: options-map
(defn ->exec [xml-streamable]
  (with-open [stream (io/input-stream xml-streamable)]
    (-> (merger/parse-to-tokens-seq stream)
        (cleanup/process)
        (select-keys [:variables :dynamic? :executable :fragments]))))


(defn load-template-model [^File dir, options-map]
  (assert (.exists dir))
  (assert (.isDirectory dir))
  (assert (map? options-map))
  (let [package-rels (relations/parse (file dir "_rels" ".rels"))
        main-document (some #(when (= rel-type-main (::type %)) (::target %)) (vals package-rels))
        ->rels (fn [f]
                 (let [rels-path (unix-path (file (.getParentFile (file f)) "_rels" (str (.getName (file f)) ".rels")))
                       rels-file (file dir rels-path)]
                   (when (.exists rels-file)
                     {::path rels-path, :source-file rels-file, :parsed (relations/parse rels-file)})))

        main-document-rels (->rels main-document)

        main-style-path (some #(when (= style/rel-type (::type %))
                                 (unix-path (file (.getParentFile (file main-document)) (::target %))))
                              (vals (:parsed main-document-rels)))
        ->exec (binding [merger/*only-includes* (boolean (:only-includes options-map))]
                 (bound-fn* ->exec))]
    {:content-types (parse-content-types (file dir "[Content_Types].xml"))
     :source-folder dir
     :relations     {::path (unix-path (file "_rels" ".rels"))
                     :source-file (file dir "_rels" ".rels")
                     :parsed package-rels}
     :main          {::path       main-document
                     :source-file (file dir main-document)
                     :executable  (->exec (file dir main-document))
                     :style (when main-style-path
                              {::path       main-style-path
                               :source-file (file dir main-style-path)
                               :parsed      (style/parse (file dir main-style-path))})
                     :relations main-document-rels
                     :headers+footers (doall
                                       (for [[id m] (:parsed main-document-rels)
                                             :when (#{rel-type-footer
                                                      rel-type-header
                                                      rel-type-slide}
                                                    (::type m))
                                             :let [f (file (.getParentFile (file main-document)) (::target m))]]
                                         {::path       (unix-path f)
                                          :source-file (file dir f)
                                          :executable  (->exec (file dir f))
                                          :relations   (->rels f)}))}}))


(defn load-fragment-model [dir options-map]
  (-> (load-template-model dir options-map)
      ;; Headers and footers are not used in fragments.
      (update :main dissoc :headers+footers)))


(defn- eval-model-part-exec [part data functions]
  (assert (:executable part))
  (assert (:dynamic? part))
  (expect-fragment-context!
   (let [[result fragments] (binding [*inserted-fragments* (atom #{})]
                              [(eval/eval-executable part data functions)
                               @*inserted-fragments*])]
     (swap! *inserted-fragments* into fragments)
     {:xml    result
      :fragment-names fragments
      :writer (->xml-writer result)})))


(defn- eval-model-part [part data functions]
  (assert (:executable part))
  (assert (::path part))
  (if (:dynamic? (:executable part))
    (eval-model-part-exec (:executable part) data functions)
    {:writer (resource-copier part)
     :xml-delay (delay
                 (with-open [reader (io/input-stream (:source-file part))]
                   (update (xml/parse reader) :content doall)))}))


(defn eval-template-model [template-model data functions fragments]
  (assert (:main template-model) "Should be a result of load-template-model call!")
  (assert (some? fragments))
  (binding [*current-styles*     (atom (:parsed (:style (:main template-model))))
            *inserted-fragments* (atom #{})
            *extra-files*        (atom #{})
            *all-fragments*      (into {} fragments)]
    (let [evaluate (fn [m]
                     (let [result                  (eval-model-part m data functions)
                           fragment-names          (set (:fragment-names result))]
                       (cond-> m

                         ;; create a rels file for the current xml
                         (and (seq @*extra-files*) (nil? (::path (:relations m))))
                         (assoc-in [:relations ::path]
                                   (unix-path (file (.getParentFile (file (::path m)))
                                              "_rels"
                                              (str (.getName (file (::path m))) ".rels"))))

                         ;; add relations if any
                         (seq @*extra-files*)
                         (update-in [:relations :parsed] (fnil into {})
                                    (for [relation @*extra-files*
                                          ;; TODO: itt a path erteket ki neke tolteni valami jora.
                                          :when (contains? fragment-names (:fragment-name relation))]
                                      [(:new-id relation) relation]))

                         ;; relation file will be rendered instead of copied
                         (seq @*extra-files*)
                         (update-in [:relations] dissoc :source-file)

                         :finally (assoc :result result))))]
      (-> template-model
          (update :main evaluate)
          (update-in [:main :headers+footers] (partial mapv evaluate))

          (cond-> (-> template-model :main :style)
            (assoc-in [:main :style :result] (style/file-writer template-model)))))))


;; returns a map where key is path and value is writer fn.
(defn evaled-template-model->writers-map [evaled-template-model]
  (as-> (sorted-map) result

    ;; relations files that are created on-the-fly
    (into result
          (for [m (tree-seq coll? seq evaled-template-model)
                :when (and (map? m) (not (sorted? m)) (::path m))
                :when (:parsed (:relations m))
                :when (not (:source-file (:relations m)))]
            [(::path (:relations m)) (relations/writer (:parsed (:relations m)))]))

    ;; create writer for every item where ::path is specified
    (into result
          (for [m (tree-seq coll? seq evaled-template-model)
                :when (and (map? m) (not (sorted? m)) (::path m))
                :when (not= "External" (::mode m))
                :when (not (contains? result (::path m)))]
            [(::path m) (or (:writer (:result m)) (resource-copier m))]))

    ;; find all items in all relations
    (into result
          (for [m (tree-seq coll? seq evaled-template-model)
                :when (and (map? m) (not (sorted? m)) (:relations m))
                :let [src-parent  (delay (file (or (:source-folder m)
                                                  (.getParentFile (file (:source-file m))))))
                      path-parent (some-> m ::path file .getParentFile)]
                relation (vals (:parsed (:relations m)))
                :when (not= "External" (::mode relation))
                :let [path (unix-path (.toFile (.normalize (.toPath (file path-parent (::target relation))))))]
                :when (or (:writer relation) (not (contains? result path)))
                :let [src (or (:source-file relation) (file @src-parent (::target relation)))]]
            [path (or (:writer relation)
                      (resource-copier {::path path :source-file src}))]))))


(defn template-model->writers-map
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


(defmethod eval/eval-step :cmd/include [f local-data-map {frag-name :name}]
  (assert (map? local-data-map))
  (assert (string? frag-name))
  (expect-fragment-context!
   (if-let [fragment-model (get *all-fragments* frag-name)]
     (let [;; merge style definitions from fragment
           style-ids-rename (-> fragment-model :main :style :parsed (doto assert) (style/insert-styles!))

           relation-ids-rename (relations/ids-rename fragment-model frag-name)
           relation-rename-map (into {} (map (juxt :old-id :new-id) relation-ids-rename))

           ;; evaluate
           evaled (eval-template-model fragment-model local-data-map {} {})

           ;; write back
           get-xml      (fn [x] (or (:xml x) @(:xml-delay x)))
           evaled-parts (->> evaled :main :result
                             (get-xml)
                             (extract-body-parts)
                             (map (partial relations/xml-rename-relation-ids relation-rename-map))
                             (map (partial style/xml-rename-style-ids style-ids-rename)))]
       (swap! *inserted-fragments* conj frag-name)
       (swap! *extra-files* into relation-ids-rename)
       [{:text (->FragmentInvoke {:frag-evaled-parts evaled-parts})}])
     (throw (ex-info "Did not find fragment for name!"
                     {:fragment-name frag-name
                      :all-fragment-names (set (keys *all-fragments*))})))))
