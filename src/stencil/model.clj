(ns stencil.model
  "Handling the meta-model of OOXML documents.
   See: http://officeopenxml.com/anatomyofOOXML.php
  "
  (:import [java.io File]
           [java.nio.file Files]
           [io.github.erdos.stencil.impl FileHelper])
  (:require [clojure.java.io :refer [file]]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.walk :refer [postwalk]]
            [stencil.ooxml :as ooxml]
            [stencil.eval :as eval]
            [stencil.tokenizer :as tokenizer]
            [stencil.util :refer :all]
            [stencil.cleanup :as cleanup]))

#_ (def tag-style :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/style)

#_ (def tag-based-on :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/basedOn)

#_ (def default-extensions {"jpeg" "image/jpeg", "png" "image/png"})

#_ (def main-content-type
     "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml")

#_ (def rels-content-type
     "application/vnd.openxmlformats-package.relationships+xml")

#_ (def relationship-style
     "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles")

(def rel-type-main
  "Relationship type of main document in _rels/.rels file."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument")

(def rel-type-style
  "Relationship type of style definitions in _rels/.rels file."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles")

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

;; throws error when not invoked from inside fragment context
(defmacro expect-fragment-context! [& bodies] `(do (assert *current-styles*) ~@bodies))


;; returns a {String {:type String, :target String}} structure where outer key is id.
(defn- parse-relation [rel-file]
  (let [parsed (with-open [r (io/input-stream (file rel-file))]
                 (xml/parse r))]
    (assert (= tag-relationships (:tag parsed))
            (str "Unexpected tag: " (:tag parsed)))
    (into (sorted-map)
          (for [d (:content parsed)
                :when (map? d)
                :when (= tag-relationship (:tag d))]
            [(:Id (:attrs d)) {::type   (doto (:Type (:attrs d)) assert)
                               ::target (doto (:Target (:attrs d)) assert)
                               ::mode   (:TargetMode :attrs)}]))))

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
  (if (.isDirectory cts)
    (recur (file cts "[Content_Types].xml"))
    (let [parsed (with-open [r (io/input-stream cts)] (xml/parse r))]
      {:source-file cts
       :path "[Content_Types].xml"
       :extensions (into (sorted-map)
                         (for [d (:content parsed)
                               :when (map? d)
                               :when (= "Default" (name (:tag d)))]
                           [(:Extension (:attrs d)), (:ContentType (:attrs d))]))
       :overrides (into (sorted-map)
                        (for [d (:content parsed)
                              :when (map? d)
                              :when (= "Override" (name (:tag d)))]
                          [(file (str (:PartName (:attrs d)))), (:ContentType (:attrs d))]))})))

(defn load-template-model [^File dir]
  (assert (.exists dir))
  (assert (.isDirectory dir))
  (let [package-rels (parse-relation (file dir "_rels" ".rels"))
        main-document (some #(when (= rel-type-main (::type %))
                               (::target %))
                            (vals package-rels))

        ->rels (fn [f]
                 (let [rels-path (str (file (.getParentFile (file f)) "_rels" (str (.getName (file f)) ".rels")))
                       rels-file (file dir rels-path)]
                   (when (.exists rels-file)
                     {:path rels-path, :source-file rels-file, :parsed (parse-relation rels-file)})))

        main-document-rels (->rels main-document)

        main-style-path (some #(when (= rel-type-style (::type %))
                                 (str (file (.getParentFile (file main-document)) (::target %))))
                              (vals (:parsed main-document-rels)))

        ->exec (fn [xml-file]
                 (with-open [stream (io/input-stream (file xml-file))]
                   (select-keys (cleanup/process (tokenizer/parse-to-tokens-seq stream))
                                [:variables :dynamic? :executable])))]
    {:content-types (parse-content-types dir)
     :source-folder dir
     :relations     {:path (str (file "_rels" ".rels"))
                     :source-file (file dir "_rels" ".rels")
                     :parsed package-rels}
     :main          {:path        main-document
                     :source-file (file dir main-document)
                     :executable  (->exec (file dir main-document))
                     :style {:path        main-style-path
                             :source-file (file dir main-style-path)
                             :parsed      (parse-style (file dir main-style-path))
                             ;; TODO: xml es tartsai?
                             :xml :TODO/xml
                             }
                     :relations main-document-rels
                     :headers+footers (doall
                                       (for [[id m] (:parsed main-document-rels)
                                             :when (#{"http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer"
                                                      "http://schemas.openxmlformats.org/officeDocument/2006/relationships/header"}
                                                    (::type m))
                                             :let [f (file (.getParentFile (file main-document)) (::target m))]]
                                         {:path        (str f)
                                          :source-files (file dir f)
                                          :executable  (->exec (file dir f))
                                          :relations   (->rels f)}))}}))

(defn load-fragment-model [dir]
  (-> (load-template-model dir)
      ;; headers and footers are not used for fragments
      ;; TODO: also remove them from relations maybe.
      (update :main dissoc :headers+footers)))

(defn- eval-model-part [part data functions]
  (assert (:executable part))
  ;; TODO: itt el kell agazni attol fuggoen h dinamikus v sem.
  (let [result (;; TODO:
                (eval 'stencil.tree-postprocess/postprocess)
                (tokenizer/tokens-seq->document
                 (eval/normal-control-ast->evaled-seq data functions (:executable part))))]
    {:xml    result
     :writer (fn [output-stream]
               (let [writer (io/writer output-stream)]
                 (xml/emit result writer)
                 (.flush writer)))}))

(defn- style-file-writer [template]
  (expect-fragment-context!
   (let [original-style-file (:source-file (:style (:main template)))
         _ (assert (.exists original-style-file))
         extended-tree (with-open [r (io/input-stream original-style-file)]
                         (let [tree (xml/parse r)
                               all-ids (set (keep (comp ooxml/style-id :attrs) (:content tree)))
                               insertable (vals (apply dissoc @*current-styles* all-ids))]
                           (update tree :content (comp doall concat) insertable)))]
     {:writer (fn [output-stream]
                (let [writer (io/writer output-stream)]
                  (xml/emit extended-tree writer)
                  (.flush writer)))})))

(defn eval-template-model [template-model data functions fragments]
  (assert (:main template-model) "Should be a result of load-template-model call!")
  (assert (some? fragments))
  (binding [*current-styles* (atom (:parsed (:style (:main template-model))))
            *inserted-fragments* (atom #{})
            *all-fragments* (into {} fragments)]
    (let [evaluate (fn [m] (assoc m :result (eval-model-part (:executable m) data functions)))]
      (->
       template-model

       (update :main evaluate)

       (update-in [:main :headers+footers] (partial mapv evaluate))

       (assoc-in [:main :style :result] (style-file-writer template-model))))))

(defn- resource-copier [x]
  (assert (:path x))
  (assert (:source-file x))
  (fn [writer]
    (let [stream (io/output-stream writer)]
      (Files/copy (.toPath (io/file (:source-file x))) stream)
      (.flush stream))))

(defn evaled-template-model->writers-map [evaled-template-model]


  (as-> (sorted-map) result

    ;; create writer for every item where :path is specified
    (into result
          (for [m (tree-seq coll? seq evaled-template-model)
                :when (and (map? m) (not (sorted? m)) (:path m))]
            [(:path m) (or (:writer (:result m)) (resource-copier m))]))

    ;; find all items in all relations
    (into result
          (for [m (tree-seq coll? seq evaled-template-model)
                :when (and (map? m) (not (sorted? m)) (:relations m))
                :let [src-parent  (file (or (:source-folder m)
                                            (.getParentFile (file (:source-file m)))))
                      path-parent (some-> m :path file .getParentFile)]
                relation (vals (:parsed (:relations m)))
                :let [path (str (file path-parent (::target relation)))]
                :when (not (contains? result path))
                :let [src (file src-parent (::target relation))]]
            [path (resource-copier {:path path :source-file src})]))))


(defn- extract-body-parts [xml-tree]
  (assert (:tag xml-tree))
  (doall
   (for [body (:content xml-tree)
         :when (= "body" (name (:tag body)))
         elem (:content body)
         :when (not= "sectPr" (name (:tag elem)))]
     elem)))


(defn- -insert-style! [style-definition]
  "Returns possibly new style id."
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

(defn insert-styles!
  ;; visszaadja az osszes stilus definiciot.
  [style-defs]
  (assert (map? style-defs) (str "Not map: " (pr-str style-defs) (type style-defs)))
  (assert (every? string? (keys style-defs)))

  (reduce (fn [m [id style]]
            (let [id2 (-insert-style! style)]
              (if (= id id2) m (assoc m id id2))))
          {} style-defs))

(defn- executable-rename-style-ids [executable style-id-renames]
  (assert (sequential? executable)
          (str "Not sequential: " (pr-str executable)))
  (assert (map? style-id-renames))
  (assert (every? string? (keys style-id-renames)))
  (assert (every? string? (vals style-id-renames)))
  (doall (for [item executable]
           (if (some-> (or (:open item) (:open+close item)) (str) (.endsWith "Style"))
             (update-some item [:attrs ooxml/val] style-id-renames)
             item))))


(defn insert-fragment! [frag-name local-data-map]
  (assert (string? frag-name))
  (assert (map? local-data-map))
  (expect-fragment-context!
   (if-let [fragment-model (get *all-fragments* frag-name)]
     (do (assert (map? local-data-map))
         (let [;; merge style definitions from fragment
               style-ids-rename (-> fragment-model :main :style :parsed (doto assert) (insert-styles!))
               fragment-model   (update-in fragment-model [:main :executable :executable]
                                           executable-rename-style-ids style-ids-rename)

               ;; evaluate
               evaled (eval-template-model fragment-model local-data-map {} {})

               ;; write back
               evaled-parts (-> evaled :main :result :xml (doto assert) extract-body-parts)]
           (swap! *inserted-fragments* conj frag-name)
           {:frag-evaled-parts evaled-parts}))
     (assert false "Did not find fragment for name!"))))

;; TODO: will need to use!
(defn- write-relation [relation-map]
  (assert (map? relation-map))
  (assert (every? string? (keys relation-map)))
  (assert (every? (comp #{#{::type ::mode ::target}} set keys) (vals relation-map)))
  (fn [output-stream]
    (let [writer (io/writer output-stream)]
      (-> {:tag tag-relationships
           :content (for [[k v] relation-map]
                      {:tag tag-relationship
                       :attrs {:Type (::type v)
                               :Target (::target v)
                               :TargetMode (::mode v)
                               :Id k}})}
          (xml/emit writer))
      (.flush writer))))

;; TODO: a relaciokat mar fragment generalaskor atnevezzuk es fuzzoljuk
;; es igy nem kell az utkozesekkel foglalkkozni
;; csak amikor osszerakjuk a dokumentumot a vegen
;; akkor bele kell tenni a kapcsolodo fajlokat is es a plusz relaciokat!
;; meg esetleg a content-type ertekeket, amik lehet h hianyoznak.


#_
(defn- update-child-tag-attr [xml tag attr update-fn]
  (->> (fn [child] (if (= tag (:tag child)) (update-in child [:attrs attr] update-fn) child))
       (partial mapv)
       (update xml :content)))

(comment

  (-> (load-template-model (file "/home/erdos/example-with-image-in-footer"))
      (eval-template-model {} {} {})
      (evaled-template-model->writers-map)
      keys sort
      time)

  (->
   (load-template-model (file "/home/erdos/example-with-image-in-footer"))
                                        ; (load-template-model (file "/home/erdos/stencil/test-resources/multipart/main.docx"))
   (assoc-in [:content-types] :CT)
   (assoc-in [:rels] :RELS)
   (assoc-in [:main :relations :parsed] :RELS)
   (assoc-in [:main :executable :executable] :EXEC)
   (assoc-in [:main :executable :variables] :EXEC/vars)
   (update-in [:main :headers+footers] (partial mapv #(assoc-in % [:executable :executable] :EXEC)))
   ;; (update-in [:main :headers+footers] (partial mapv #(assoc-in % [:relations :parsed] :PARSED/RELS)))
   (clojure.pprint/pprint)
   time)

  comment)
