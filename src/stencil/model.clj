(ns stencil.model
  (:require [clojure.java.io :refer [file]]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]))

;; we need a method to rewrite relations in

;; http://officeopenxml.com/anatomyofOOXML.php
;;
;;
;;

(def default-extensions {"jpeg" "image/jpeg", "png" "image/png"})

(def main-content-type
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml")

(def rels-content-type
  "application/vnd.openxmlformats-package.relationships+xml")

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

(defn merge-styles [& style-xmls]
  ;;q
  )

#_
(defn parse-package [pkg]
  (let [{:keys [defaults overrides]}
        (parse-content-types (file pgs "[Content Types].xml"))

        rels-files (for [[k v] overrides
                         :when (= v rels-content-type)] (file pkg k))
        rels (parse-rels (file pgs "_rels/.rels"))
        ]


    ))


;; returned keys:
;; :id -
;; :target -
;; :type -
;; :target-mode -

;; az osszes relaciot atmapeljuk generalt ertekekre.
;;

;;
#_
(defn emit-rels [rels])
