(ns stencil.model
  (:import [java.io File]
           [io.github.erdos.stencil.impl FileHelper])
  (:require [clojure.java.io :refer [file]]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.walk :refer [postwalk]]
            [stencil.ooxml :as ooxml]
            [stencil.eval :as eval]
            [stencil.tokenizer :as tokenizer]
            [stencil.cleanup :as cleanup]))

;; http://officeopenxml.com/anatomyofOOXML.php

(def tag-style :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/style)

(def tag-based-on :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/basedOn)

(def default-extensions {"jpeg" "image/jpeg", "png" "image/png"})

(def main-content-type
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml")

(def rels-content-type
  "application/vnd.openxmlformats-package.relationships+xml")

(def relationship-style
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles")

;; style definitions of the main document
(def ^:dynamic ^:private *current-styles* nil)

;; all insertable fragments
(def ^:private ^:dynamic *all-fragments* nil)

(defn- update-child-tag-attr [xml tag attr update-fn]
  (->> (fn [child] (if (= tag (:tag child)) (update-in child [:attrs attr] update-fn) child))
       (partial mapv)
       (update xml :content)))

(defn- parse-content-types [cts]
  (assert cts)
  (let [parsed (with-open [r (io/input-stream cts)] (xml/parse r))]
    {:extensions (into (sorted-map)
                       (for [d (:content parsed)
                             :when (map? d)
                             :when (= "Default" (name (:tag d)))]
                         [(:Extension (:attrs d)), (:ContentType (:attrs d))]))
     :overrides (into (sorted-map)
                      (for [d (:content parsed)
                            :when (map? d)
                            :when (= "Override" (name (:tag d)))]
                        [(file (str (:PartName (:attrs d)))), (:ContentType (:attrs d))]))}))

(defn get-mime-type [cts f]
  (assert (:extensions cts))
  (assert (:overrides cts))
  (assert false "Nincs ilyen fajl!"))

; (clojure.pprint/pprint (parse-content-types (file "/home/erdos/Downloads/[Content_Types].xml")))

(defn fuzz-cts [cts]
  (assert (:extensions cts))
  (assert (:overrides cts))
  {:extensions (:extensions cts)
   :overrides (:overrides cts)
   :file-renamer (fn [f] f)})

(defn merge-font-tables [& font-table-xmls]
  (assert (every? (comp #{"fonts"} name :tag) font-table-xmls))
  (let [fonts (mapv :content font-table-xmls)
        font-names (mapv () ()) ;; vector of sets

        ]

    ;; new (merged) font table xml. contains original font names where possible,
    ;; contains renamed font names otherwise.
    {:xml nil
     ;; font name renames
     :name-renames [{}]
     }))

;; returns a {String {:type String, :target String}} structure where outer key is id.
(defn- parse-relation [rel-file]
  (let [parsed (with-open [r (io/input-stream (file rel-file))] (xml/parse r))]
    (with-meta
      (into (sorted-map)
            (for [d (:content parsed)
                  :when (map? d)
                  :when (= "Relationship" (name (:tag d)))]
              [(:Id (:attrs d))
               ;; TODO: maybe target type too!
               {:type (:Type (:attrs d)), :target (:Target (:attrs d))}]))
      {:t :parsed-relation})))

;; (parse-relation (file "/home/erdos/Downloads/word/_rels/document.xml.rels"))


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

(defn merge-relations
  ;; atnevezi az id-ket es a fajlokat.
  [& relations]
  (doseq [r relations] (#{:parsed-relation} (:t (meta r))))
  (let [trees ()]
    {
     ;; unified XML tree
     :xml (update (first relations) :content into ())

     ;; mapping of id values for each tree where needed.
     ;; First tree may keep its values.
     :id-renames [{}]
     :target-renames [{}]}))

(defn- update-some [m path f]
  (if-some [x (get-in m path)]
    (if-some [fx (f x)]
      (assoc-in m path fx)
      m)
    m))

;; returns a map of {:insertable [ITEMS]} that contains insertable elements from the fragment
(defn- read-xml-main [xml-file]
  (with-open [stream (io/input-stream (file xml-file))]
    {:insertable
     (cleanup/process (tokenizer/parse-to-tokens-seq stream))}))

(defn extract-body-parts [xml-tree]
  (assert (:tag xml-tree))
  (doall
   (for [body (:content xml-tree)
         :when (= "body" (name (:tag body)))
         elem (:content body)
         :when (not= "sectPr" (name (:tag elem)))]
     elem)))

(defn- rename-style-ids [xml style-id-renames]
  (assert (:tag xml)
          (str "Unexpected xml: "
               (pr-str xml)))
  (assert (map? style-id-renames))
  (if (empty? style-id-renames)
    xml
    (postwalk
     (fn [elem]
       (if (and (map? elem) (.endsWith (str (:tag elem)) "Style"))
         (update-some elem [:attrs ooxml/val] style-id-renames)
         elem))
     xml)))

 (defn- executable-rename-style-ids [executable style-id-renames]
   (assert (sequential? executable)
           (str "Not sequential: " (pr-str executable)))
   (assert (map? style-id-renames))
   (doall (for [item executable]
            (if (some-> (or (:open item) (:open+close item)) (str) (.endsWith "Style"))
              (update-some item [:attrs ooxml/val] style-id-renames)
              item))))


(defn prepare-document-file [^File xml-file]
  (assert (.exists xml-file))
  (assert (.isFile xml-file))
  (assert (.endsWith (.getName xml-file) ".xml"))
  (let [main-rels-file (file "_rels" (str (.getName xml-file) ".rels"))
        main-rels (parse-relation (file (.getParentFile xml-file) main-rels-file))
        style-file (some (fn [[id {:keys [type target]}]]
                           (when (= relationship-style type)
                             (file target))) main-rels)]
    (assert (.exists (file (.getParentFile xml-file) main-rels-file)))
    (assert (.exists (file (.getParentFile xml-file) style-file)))
    {:xml-file   xml-file
     :rels-file  main-rels-file
     :rels       main-rels
     :style-file (file (.getParentFile xml-file) style-file)}))


(defn prepare-document [^File dir]
  (assert (.exists dir) (str "Does not exist: " dir))
  (assert (.isDirectory dir))
  (let [content-types (parse-content-types (file dir "[Content_Types].xml"))
        main-file (some #(when (= main-content-type (val %))
                           (file dir (.substring (str (key %)) 1)))
                        (:overrides content-types))
        prepared (prepare-document-file main-file)]
    {:main-file       (:xml-file prepared)
     :main-rels-file  (:rels-file prepared)
     :main-rels       (:rels prepared)
     :main-style-file (:style-file prepared)
     :main-style-path (->> (:style-file prepared)
                           (file)
                           (.toPath)
                           (.relativize (.toPath dir))
                           (FileHelper/toUnixSeparatedString))
     :main-style-definitions (parse-style (:style-file prepared))}))

(defn prepare-fragment [^File dir]
  (let [document (prepare-document dir)]
    {:frag-main-file       (:main-file document)
     :frag-main-rels-file  (:main-rels-file document)
     :frag-main-rels       (:main-rels document)
     :frag-main-style-file (:main-style-file document)

     :frag-style-definitions (parse-style (:main-style-file document))

     ;; content
     :frag-xml (read-xml-main (:main-file document))

     :files-to-add {}}))

;; visszaad egy fuggvenyt ami beleir a writer-be!
(defn write-main-style-file [main-document]
  (assert *current-styles*)
  (let [original-style-file (file (:main-style-file main-document))
        extended-tree (with-open [r (io/input-stream original-style-file)]
                        (let [tree (xml/parse r)
                              all-ids (set (keep (comp ooxml/style-id :attrs) (:content tree)))
                              insertable (vals (apply dissoc @*current-styles* all-ids))]
                          (update tree :content concat insertable)))]
    (fn [output-stream]
      (let [writer (io/writer output-stream)]
        (xml/emit extended-tree writer)
        (.flush writer)))))

;; inserts style definition to current document, returns style id (maybe generated new)
(defn- insert-style! [style-definition]
  (assert (= "style" (name (:tag style-definition))))
  (assert (contains? (:attrs style-definition) ooxml/style-id))
  (assert *current-styles*)
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
          id))))

(defn insert-styles!
  ;; visszaadja az osszes stilus definiciot.
  [style-file]
  (assert (instance? java.io.File style-file))
  (let [style-defs (parse-style style-file)]
    (assert style-defs)
    (reduce (fn [m [id style]]
              (let [id2 (insert-style! style)]
                (if (= id id2) m (assoc m id id2))))
            {} style-defs)))

(defmacro fragment-context [opts & bodies]
  (assert (map? opts))
  (assert (:main opts))
  `(binding [*current-styles* (atom {})
             *all-fragments*  (into {} (or ~(:fragments opts)
                                           (assert false "Fragment map must not be empty!")))]
     ;; todo: itt a fo stilus fajlt be kell olvasni!
     (insert-styles! (or (:main-style-file ~(:main opts))
                         (assert false "main-style-file is missing!")))
     ~@bodies))

(defn- insert-fragment-impl [frag-map local-data-map]
  (assert (:frag-xml frag-map))
  (assert (map? local-data-map))
  (let [style-ids-rename (insert-styles! (:frag-main-style-file frag-map))
        executable (-> frag-map :frag-xml :insertable :executable
                       (executable-rename-style-ids style-ids-rename))
        evaled-seq (eval/normal-control-ast->evaled-seq local-data-map {} executable)
        evaled-tree (tokenizer/tokens-seq->document evaled-seq)
        evaled-tree-parts (extract-body-parts evaled-tree)]
    {:frag-evaled-tree evaled-tree
     :frag-evaled-parts evaled-tree-parts}))

(defn insert-fragment! [frag-name local-data-map]
  (assert (string? frag-name))
  (assert (map? local-data-map))
  (assert *all-fragments* "Nem fragment kontextusban vagyunk!")
  (if-let [frag-map (get *all-fragments* frag-name)]
    (insert-fragment-impl frag-map local-data-map)
    (assert false "Did not find fragment for name!")))

(defn get-additional-writers-map
  "Collects fragment parts. Returns a map of relative paths and writers."
  [main-document]
  (assert @*current-styles* "This function must be called from a fragment context!")
  ;; itt ellenorzunk arra, hogy a history valtozott-e. ha igen, akkor
  (let [main-style-file (:main-style-path main-document)]
    (assert main-style-file)
    { main-style-file (write-main-style-file main-document)}))


; (do (prepare-fragment (file "/home/erdos/Downloads")) :ok)
