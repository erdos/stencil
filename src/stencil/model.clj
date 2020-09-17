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
            [stencil.ooxml :as ooxml]
            [stencil.eval :as eval]
            [stencil.merger :as merger]
            [stencil.tree-postprocess :as tree-postprocess]
            [stencil.types :refer [->FragmentInvoke]]
            [stencil.util :refer :all]
            [stencil.cleanup :as cleanup]))

(set! *warn-on-reflection* true)

(def rel-type-main
  "Relationship type of main document in _rels/.rels file."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument")

(def rel-type-style
  "Relationship type of style definitions in _rels/.rels file."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles")

(def rel-type-footer
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer")

(def rel-type-header
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/header")

(def rel-type-slide
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide")

(def rel-type-image
  "Relationship type of image files in .rels files."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image")

(def rel-type-hyperlink
    "Relationship type of hyperlinks in .rels files."
    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink")

(def tag-relationships
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationships)

(def tag-relationship
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationship)

;; style definitions of the main document
(def ^:dynamic ^:private *current-styles* nil)

;; all insertable fragments. map of id to frag def.
(def ^:private ^:dynamic *all-fragments* nil)

;; set of already inserted fragment ids.
(def ^:private ^:dynamic *inserted-fragments* nil)

;; list of extra relations to be added after evaluating document
(def ^:private ^:dynamic *extra-files* nil)

;; throws error when not invoked from inside fragment context
(defmacro ^:private expect-fragment-context! [& bodies] `(do (assert *current-styles*) ~@bodies))

(defn- unix-path [^File f]
  (some-> f .toPath FileHelper/toUnixSeparatedString))

(defn- parse-relation [rel-file]
  (with-open [reader (io/input-stream (file rel-file))]
    (let [parsed (xml/parse reader)]
      (assert (= tag-relationships (:tag parsed))
              (str "Unexpected tag: " (:tag parsed)))
      (into (sorted-map)
            (for [d (:content parsed)
                  :when (map? d)
                  :when (= tag-relationship (:tag d))]
              [(:Id (:attrs d)) {::type   (doto (:Type (:attrs d)) assert)
                                 ::target (doto (:Target (:attrs d)) assert)
                                 ::mode   (:TargetMode (:attrs d))}])))))


(defn- parse-style
  "Returns a map where key is style id and value is style definition."
  [style-file]
  (assert style-file)
  (with-open [r (io/input-stream (file style-file))]
    (into (sorted-map)
          (for [d (:content (xml/parse r))
                :when (map? d)
                :when (= ooxml/style (:tag d))]
            [(ooxml/style-id (:attrs d)) d]))))


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
  (let [package-rels (parse-relation (file dir "_rels" ".rels"))
        main-document (some #(when (= rel-type-main (::type %)) (::target %)) (vals package-rels))
        ->rels (fn [f]
                 (let [rels-path (unix-path (file (.getParentFile (file f)) "_rels" (str (.getName (file f)) ".rels")))
                       rels-file (file dir rels-path)]
                   (when (.exists rels-file)
                     {::path rels-path, :source-file rels-file, :parsed (parse-relation rels-file)})))

        main-document-rels (->rels main-document)

        main-style-path (some #(when (= rel-type-style (::type %))
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
                               :parsed      (parse-style (file dir main-style-path))})
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


(defn ->xml-writer [tree]
  (fn [output-stream]
    (io!
     (let [writer (io/writer output-stream)]
       (xml/emit tree writer)
       (.flush writer)))))


(defn- resource-copier [x]
  (assert (::path x))
  (assert (:source-file x))
  (fn [writer]
    (io!
     (let [stream (io/output-stream writer)]
       (Files/copy (.toPath (io/file (:source-file x))) stream)
       (.flush stream)
       nil))))


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


(defn- style-file-writer [template]
  (expect-fragment-context!
   (let [original-style-file (:source-file (:style (:main template)))
         _ (assert (.exists ^File original-style-file))
         extended-tree (with-open [r (io/input-stream original-style-file)]
                         (let [tree (xml/parse r)
                               all-ids (set (keep (comp ooxml/style-id :attrs) (:content tree)))
                               insertable (vals (apply dissoc @*current-styles* all-ids))]
                           (update tree :content concatv insertable)))]
     {:writer (->xml-writer extended-tree)})))


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
            (assoc-in [:main :style :result] (style-file-writer template-model)))))))


(defn- relation-writer [relation-map]
  (assert (map? relation-map))
  (assert (every? string? (keys relation-map)) (str "Not all str: " (keys relation-map)))
  (->
   {:tag tag-relationships
    :content (for [[k v] relation-map]
               {:tag tag-relationship
                :attrs (cond-> {:Type (::type v), :Target (::target v), :Id k}
                         (::mode v) (assoc :TargetMode (::mode v)))})}
   ;; LibreOffice opens the generated document only when default xml namespace is the following:
   (with-meta {:clojure.data.xml/nss
               (pu/assoc pu/EMPTY "" "http://schemas.openxmlformats.org/package/2006/relationships")})
   (->xml-writer)))

;; returns a map where key is path and value is writer fn.
(defn evaled-template-model->writers-map [evaled-template-model]
  (as-> (sorted-map) result

    ;; relations files that are created on-the-fly
    (into result
          (for [m (tree-seq coll? seq evaled-template-model)
                :when (and (map? m) (not (sorted? m)) (::path m))
                :when (:parsed (:relations m))
                :when (not (:source-file (:relations m)))]
            [(::path (:relations m)) (relation-writer (:parsed (:relations m)))]))

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


(defn- -insert-style!
  "Returns possibly new style id."
  [style-definition]
  (assert (= "style" (name (:tag style-definition))))
  (assert (contains? (:attrs style-definition) ooxml/style-id))
  (expect-fragment-context!
   (let [id (-> style-definition :attrs ooxml/style-id)]
     (if-let [old-style (get @*current-styles* id)]
       (if (= old-style style-definition)
         id
         (let [new-id    (name (gensym "sid"))
               new-style (assoc-in style-definition [:attrs ooxml/style-id] new-id)
               new-style (update new-style :content
                                 #(for [c %]
                                    ;; TODO: itt leptetni kell majd,
                                    ;; hogy a nev rendes erteket kapjon!!!!
                                    (if (= ooxml/name (:tag c))
                                      (assoc-in c [:attrs ooxml/val] (name (gensym "title")))
                                      c)))]
           (swap! *current-styles* assoc new-id new-style)
           new-id))
       (do (swap! *current-styles* assoc id style-definition)
           id)))))


(defn- insert-styles!
  "Returns a map of all style definitions where key is style id and value is style xml."
  [style-defs]
  (assert (map? style-defs) (str "Not map: " (pr-str style-defs) (type style-defs)))
  (assert (every? string? (keys style-defs)))
  (reduce (fn [m [id style]]
            (let [id2 (-insert-style! style)]
              (if (= id id2) m (assoc m id id2))))
          {} style-defs))


(defn xml-rename-style-ids [style-id-renames xml-tree]
  (if (map? xml-tree)
    (if (-> xml-tree :tag name (.endsWith "Style"))
      (update-some xml-tree [:attrs ooxml/val] style-id-renames)
      (update xml-tree :content (partial map (partial xml-rename-style-ids style-id-renames))))
    xml-tree))


(defn- map-rename-relation-ids [item id-rename]
  (-> item
      ;; Image relation ids are being renamed here.
      (update-some [:attrs ooxml/r-embed] id-rename)
      ;; Hyperlink relation ids are being renamed here
      (update-some [:attrs ooxml/r-id] id-rename)))


(defn- xml-rename-relation-ids [id-rename xml-tree]
  (if (map? xml-tree)
    (-> xml-tree
        (map-rename-relation-ids id-rename)
        (update :content (partial map (partial xml-rename-relation-ids id-rename))))
    xml-tree))

;; generates a random relation id
(defn- ->relation-id [] (str (gensym "stencilRelId")))


(defn- relation-ids-rename [model fragment-name]
  (doall
   (for [[old-rel-id m] (-> model :main :relations :parsed (doto assert))
         :when (#{rel-type-image rel-type-hyperlink} (::type m))
         :let [new-id       (->relation-id)
               new-path     (if (= "External" (::mode m))
                              (::target m)
                              (str new-id "." (last (.split (str (::target m)) "\\."))))]]
     {::type       (::type m)
      ::mode       (::mode m)
      ::target     new-path
      :fragment-name fragment-name
      :new-id      new-id
      :old-id      old-rel-id
      :source-file (file (-> model :main :source-file file .getParentFile) (::target m))
      ::path       new-path})))


(defmethod eval/eval-step :cmd/include [f local-data-map {frag-name :name}]
  (assert (map? local-data-map))
  (assert (string? frag-name))
  (expect-fragment-context!
   (if-let [fragment-model (get *all-fragments* frag-name)]
     (let [;; merge style definitions from fragment
           style-ids-rename (-> fragment-model :main :style :parsed (doto assert) (insert-styles!))

           relation-ids-rename (relation-ids-rename fragment-model frag-name)
           relation-rename-map (into {} (map (juxt :old-id :new-id) relation-ids-rename))

           ;; evaluate
           evaled (eval-template-model fragment-model local-data-map {} {})


           ;; write back
           get-xml      (fn [x] (or (:xml x) @(:xml-delay x)))
           evaled-parts (->> evaled :main :result
                             (get-xml)
                             (extract-body-parts)
                             (map (partial xml-rename-relation-ids relation-rename-map))
                             (map (partial xml-rename-style-ids style-ids-rename)))]
       (swap! *inserted-fragments* conj frag-name)
       (swap! *extra-files* into relation-ids-rename)
       [{:text (->FragmentInvoke {:frag-evaled-parts evaled-parts})}])
     (throw (ex-info "Did not find fragment for name!"
                     {:fragment-name frag-name
                      :all-fragment-names (set (keys *all-fragments*))})))))
