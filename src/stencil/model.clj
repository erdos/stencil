(ns stencil.model
  "Handling the meta-model of OOXML documents.
   See: http://officeopenxml.com/anatomyofOOXML.php
  "
  (:import [java.io File]
           [java.nio.file Files]
           [io.github.erdos.stencil.impl FileHelper])
  (:require [clojure.java.io :refer [file]]
            [clojure.data.xml :as xml]
            [clojure.data.xml.pu-map :as pu]
            [clojure.java.io :as io]
            [clojure.walk :refer [postwalk]]
            [stencil.ooxml :as ooxml]
            [stencil.eval :as eval]
            [stencil.tokenizer :as tokenizer]
            [stencil.util :refer :all]
            [stencil.cleanup :as cleanup]))

(def rel-type-main
  "Relationship type of main document in _rels/.rels file."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument")

(def rel-type-style
  "Relationship type of style definitions in _rels/.rels file."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles")

(def rel-type-image
  "Relationship type of image files in .rels files."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image")

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

(def ^:private ^:dynamic *extra-files* nil)

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
                             :parsed      (parse-style (file dir main-style-path))}
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

(defn ->xml-writer [tree]
  (fn [output-stream]
    (let [writer (io/writer output-stream)]
      (xml/emit tree writer)
      (.flush writer))))

(defn- eval-model-part [part data functions]
  (assert (:executable part))
  ;; TODO: itt el kell agazni attol fuggoen h dinamikus v sem.
  (let [[result fragments] (binding [*inserted-fragments* (atom #{})]
                             [(;; TODO:
                               (eval 'stencil.tree-postprocess/postprocess)
                               (tokenizer/tokens-seq->document
                                (eval/normal-control-ast->evaled-seq data functions (:executable part))))
                              @*inserted-fragments*])]
    (swap! *inserted-fragments* into fragments)
    {:xml    result
     :fragment-names fragments
     :writer (->xml-writer result)}))

(defn- style-file-writer [template]
  (expect-fragment-context!
   (let [original-style-file (:source-file (:style (:main template)))
         _ (assert (.exists original-style-file))
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
                     (let [result (eval-model-part (:executable m) data functions)
                           fragment-names          (set (:fragment-names result))]
                       ;; ha a fragment-names nem ures, akkor a fragmentet megfuzzoljuk
                       ;; tehat hozzaadjuk a fragmentbol a kapcsolodo relaciokat valahogy!!!!!

                       (cond-> m

                         (and (seq @*extra-files*) (nil? (:path (:relations m))))
                         (assoc-in [:relations :path]
                                   ;; itt ki kell talalni h a relacio utvonala mi legyen...
                                   (str (file (.getParentFile (file (:path m)))
                                              "_rels"
                                              (str (.getName (file (:path m))) ".rels"))))

                           ;; Az adott reszdokumentumon vegigmegyunk es beletoljuk azokat a resz doksi reszeket amik relevansak.
                         (seq @*extra-files*)
                         (update-in [:relations :parsed] (fnil into {})
                                    (for [relation @*extra-files*
                                          ;; TODO: itt a path erteket ki neke tolteni valami jora.
                                          :when (contains? fragment-names (:fragment-name relation))]
                                      [(:new-id relation) relation]))

                         ;; Az extra-files reszen vegigmegyunk es szepen a sajat relaciok ala betoljuk
                         ;; azokat a relaciokat ahol a fragment-names stimmel.
                         :finally (assoc :result result))))]
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

(defn- relation-writer [relation-map]
  (assert (map? relation-map))
  (assert (every? string? (keys relation-map)) (str "Not all str: " (keys relation-map)))
  (->
   {:tag tag-relationships
    :content (for [[k v] relation-map]
               {:tag tag-relationship
                :attrs (cond-> {:Type (::type v), :Target (::target v), :Id k}
                         (::mode v) (assoc :TargetMode (::mode v)))})}
   ;; LibreOffice opens the generated document only when default namespace in the following.
   (with-meta {:clojure.data.xml/nss (pu/assoc pu/EMPTY "" "http://schemas.openxmlformats.org/package/2006/relationships")})
   (->xml-writer)))

(defn evaled-template-model->writers-map [evaled-template-model]


  (as-> (sorted-map) result

    ;; relations files that are created on-the-fly
    (into result
          (for [m (tree-seq coll? seq evaled-template-model)
                :when (and (map? m) (not (sorted? m)) (:path m))
                :when (:parsed (:relations m))
                :when (not (:source-file (:relations m)))]
            [(:path (:relations m)) (relation-writer (:parsed (:relations m)))]))

    ;; create writer for every item where :path is specified
    (into result
          (for [m (tree-seq coll? seq evaled-template-model)
                :when (and (map? m) (not (sorted? m)) (:path m))
                :when (not (contains? result (:path m)))]
            [(:path m) (or (:writer (:result m)) (resource-copier m))]))

    ;; find all items in all relations
    (into result
          (for [m (tree-seq coll? seq evaled-template-model)
                :when (and (map? m) (not (sorted? m)) (:relations m))
                :let [src-parent  (delay (file (or (:source-folder m)
                                                  (.getParentFile (file (:source-file m))))))
                      path-parent (some-> m :path file .getParentFile)]
                relation (vals (:parsed (:relations m)))
                :let [path (str (file path-parent (::target relation)))]
                :when (or (:writer relation) (not (contains? result path)))
                :let [src (or (:source-file relation) (file @src-parent (::target relation)))]]
            [path (or (:writer relation)
                      (resource-copier {:path path :source-file src}))]))))


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

(defn- executable-rename-relation-ids [executable id-rename]
  (assert (sequential? executable))
  (assert (map? id-rename))
  (assert (every? string? (keys id-rename)))
  (assert (every? string? (vals id-rename)))
  (doall (for [item executable]
           ;; images are being renamed
           (update-some item [:attrs ooxml/embed] id-rename))))

(defn- relation-ids-rename [model]
  (doall
   (for [[old-rel-id m] (-> model :main :relations :parsed (doto assert))
         :when (= rel-type-image (::type m))
         :let [new-id       (str (java.util.UUID/randomUUID))
               extension    (last (.split (str (::target m)) "\\."))
               new-path     (str new-id "." extension)]]
     {::type       (::type m)
      ::target     new-path
      ::mode       (::mode m)
      :new-id      new-id
      :old-id      old-rel-id
      :source-file (file (-> model :main :source-file .getParentFile) (::target m))
      :path        new-path})))

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

               relation-ids-rename (map #(assoc % :fragment-name frag-name)
                                        (relation-ids-rename fragment-model))
               fragment-model   (update-in fragment-model [:main :executable :executable]
                                           executable-rename-relation-ids (into {} (map (juxt :old-id :new-id) relation-ids-rename)))

               ;; evaluate
               evaled (eval-template-model fragment-model local-data-map {} {})

               ;; write back
               evaled-parts (-> evaled :main :result :xml (doto assert) extract-body-parts)]
           (swap! *inserted-fragments* conj frag-name)
           (swap! *extra-files* into relation-ids-rename)
           {:frag-evaled-parts evaled-parts}))
     (assert false "Did not find fragment for name!"))))



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
