(ns stencil.model
  (:import [java.io File])
  (:require [clojure.java.io :refer [file]]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [stencil.ooxml :as ooxml]))

;; http://officeopenxml.com/anatomyofOOXML.php

(def tag-style :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/style)
(def attr-style-id :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/styleId)

(def tag-based-on :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/basedOn)

(def default-extensions {"jpeg" "image/jpeg", "png" "image/png"})

(def main-content-type
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml")

(def rels-content-type
  "application/vnd.openxmlformats-package.relationships+xml")

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
                                     new-id (gensym "sId")]
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

(defn- emit-content-types [cts])

(defn get-mime-type [cts f]
  (assert (:extensions cts))
  (assert (:overrides cts))
  (assert false "Nincs ilyen fajl!"))

(clojure.pprint/pprint (parse-content-types (file "/home/erdos/Downloads/[Content_Types].xml")))

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

;; returns a map of {:elems [ITEMS]} that contains insertable elements from the fragment
(defn- read-xml-main [xml-file]
  (with-open [r (io/input-stream (file xml-file))]
    {:elems
     (doall
      (for [body (:content (xml/parse r))
            :when (= "body" (name (:tag body)))
            elem (:content body)
            :when (not= "sectPr" (name (:tag elem)))]
        elem))}))


(defn prepare-fragment [^File dir]
  (assert (.exists dir) (str "Does not exist: " dir))
  (assert (.isDirectory dir))
  (let [content-types (parse-content-types (file dir "[Content_Types].xml"))
        main-file (some #(when (= main-content-type (val %))
                           (file dir (.substring (str (key %)) 1)))
                        (:overrides content-types))
        _ (assert main-file "No document.xml in [Content_Types].xml file!")
        main-rels-file (file (.getParentFile main-file) "_rels" (str (.getName main-file) ".rels"))
        main-rels (parse-relation main-rels-file)
        ]
    (assert (.exists main-rels-file))
    main-rels
    {:frag-main-file main-file
     :frag-main-rels-file main-rels-file
     :frag-main-rels main-rels
     :frag-main-style-file nil

     ;; xml tree of main
     :frag-xml
     ;; file path to file writer - to copy font/image files
     :files-to-add {}
     }
    ))

(prepare-fragment (file "/home/erdos/Downloads"))
