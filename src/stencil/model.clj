(ns stencil.model
  (:import [java.io File])
  (:require [clojure.java.io :refer [file]]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.walk :refer [postwalk]]
            [stencil.ooxml :as ooxml]
            [stencil.tokenizer :as tokenizer]
            [stencil.cleanup :as cleanup]))

;; http://officeopenxml.com/anatomyofOOXML.php

(def tag-style :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/style)
(def attr-style-id :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/styleId)

(def tag-based-on :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/basedOn)

(def default-extensions {"jpeg" "image/jpeg", "png" "image/png"})

(def main-content-type
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml")

(def rels-content-type
  "application/vnd.openxmlformats-package.relationships+xml")

(def relationship-style
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles")

(def ^:dynamic ^:private *current-styles* nil)

(defn- update-child-tag-attr [xml tag attr update-fn]
  (->> (fn [child] (if (= tag (:tag child)) (update-in child [:attrs attr] update-fn) child))
       (partial mapv)
       (update xml :content)))

(defn- add-styles [xml-styles xml-styles-new]
  (assert (= "styles" (name (:tag xml-styles))))
  (assert (= "styles" (name (:tag xml-styles-new))))
  ;; a jobb oldalrol
  ;; TODO: ha van egy szablay, ami epul egy masikra mit felulirunk, akkor
  ;; a raepulesnel is masikat kell hasznalni!!!
  (let [old-styles (set (filter (comp #{tag-style} :tag) (:content xml-styles)))
        new-styles (remove old-styles (filter (comp #{tag-style} :tag) (:content xml-styles-new)))
        [rename out] (reduce (fn [[rename out] new-style]
                               (let [old-id (get-in new-style [:attrs attr-style-id])
                                     ;; TODO: id generation should ensure no collisions!
                                     new-id (name (gensym "sId"))]
                                 [(assoc rename old-id new-id)
                                  (conj out (assoc-in new-style [:attrs attr-style-id] new-id))]))
                             [{} []] new-styles)
        out (for [rule out]
              (update-child-tag-attr rule tag-based-on ooxml/val (fn [x] (rename x x))))]
    {:xml (update xml-styles :content into out)
     :style-id-renames rename
     }))


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


(defn- parse-style [style-file]
  (with-open [r (io/input-stream (file style-file))]
    (into (sorted-map)
          (for [d (:content (xml/parse r))
                :when (map? d)
                ;:when (= "Relationship" (name (:tag d)))
                ]
            [(str d)
                                        ;(:Id (:attrs d))
             ;; TODO: maybe target type too!
             {:type (:Type (:attrs d)), :target (:Target (:attrs d))}]))))

; (parse-style (file "/home/erdos/Downloads/word/styles.xml"))

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
  (assert (:tag xml))
  (assert (map? style-id-renames))
  (if (empty? style-id-renames)
    xml
    (postwalk
     (fn [elem]
       (if (and (map? elem) (.endsWith (str (:tag elem)) "Style"))
         (update-some elem [:attrs ooxml/val] style-id-renames)
         elem))
     xml)))


(defn prepare-document-file [^File xml-file]
  (assert (.exists xml-file))
  (assert (.isFile xml-file))
  (assert (.endsWith (.getName xml-file) ".xml"))
  (let [main-rels-file (file (.getParentFile xml-file) "_rels" (str (.getName xml-file) ".rels"))
        main-rels (parse-relation main-rels-file)
        style-file (some (fn [[id {:keys [type target]}]]
                           (when (= relationship-style type)) target) main-rels)]
    {:xml-file   xml-file
     :rels-file  main-rels-file
     :rels       main-rels
     :style-file style-file}))


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
     :main-style-file (:style-file prepared)}))


(defn prepare-fragment [^File dir]
  (let [document (prepare-document dir)]
    {:frag-main-file       (:main-file document)
     :frag-main-rels-file  (:main-rels-file document)
     :frag-main-rels       (:main-rels document)
     :frag-main-style-file (:main-style-file document)

     ;; content
     :frag-xml (read-xml-main (:main-file document))

     :files-to-add {}}))

;; visszaad egy fuggvenyt ami beleir a writer-be!
(defn write-main-style-file [main-document]
  (assert (:main-file main-document))
  (assert *current-styles*)
  (let [main-style (:frag-main-style-file main-document)
        extra-styles (vals @*current-styles*)]
    (fn [writer]
      ;; itt a stiluslap faba beleinzertalunk tovabbi stilus definiciokat es minden kiraly.
      )))

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
              new-style (assoc-in style-definition [:attrs ooxml/style-id] new-id)]
          (swap! *current-styles* assoc new-id new-style)
          new-id))
      (do (swap! *current-styles* assoc id style-definition)
          id))))

(defmacro fragment-context [& bodies]
  `(binding [*current-styles* (atom {})]
     ~@bodies))

(defn insert-fragment! [frag-map]
  (assert (:frag-xml frag-map))
  (let [style-ids-rename
        (reduce (fn [m [id style]]
                  (let [id2 (insert-style! style)]
                    (if (= id id2) m (assoc m id id2))))
                {} (:frag-style-definitions frag-map))
        frag-xml (rename-style-ids (:frag-xml frag-map) style-ids-rename)]
    {:frag-xml frag-xml}))


(do (prepare-fragment (file "/home/erdos/Downloads")) :ok)
