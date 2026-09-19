(ns stencil.model.relations
  (:require [clojure.data.xml :as xml]
            [clojure.data.xml.pu-map :as pu]
            [clojure.java.io :as io :refer [file]]
            [stencil.fs :as fs :refer [unix-path]]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer [update-some find-first]]
            [stencil.model.common :refer [->xml-writer]]))

(def tag-relationships
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationships)

(def tag-relationship
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationship)

(def rel-type-hyperlink
  "Relationship type of hyperlinks in .rels files."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink")

(def rel-type-image
  "Relationship type of image files in .rels files."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image")

(def rel-type-main
  "Relationship type of main document in _rels/.rels file."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument")

(def rel-type-footer
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer")

(def rel-type-header
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/header")

;; PPTX

(def rel-type-slide
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide")

(def rel-type-slide-master
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster")

(def rel-type-slide-layout
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout")

(def rel-type-theme
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme")

(def rel-type-notes-slide
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/notesSlide")

(def rel-type-notes-master
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/notesMaster")

(def extra-relations
  #{rel-type-footer rel-type-header rel-type-slide rel-type-slide-master rel-type-notes-master})

(defn- parse [rel-file]
  (with-open [reader (io/input-stream (file rel-file))]
    (let [parsed (xml/parse reader)]
      (assert (= tag-relationships (:tag parsed))
              (str "Unexpected tag: " (:tag parsed)))
      (into (sorted-map)
            (for [d (:content parsed)
                  :when (map? d)
                  :when (= tag-relationship (:tag d))]
              [(:Id (:attrs d)) {:stencil.model/type   (doto (:Type (:attrs d)) assert)
                                 :stencil.model/target (doto (:Target (:attrs d)) assert)
                                 :stencil.model/mode   (:TargetMode (:attrs d))}])))))

(defn ->rels [^java.io.File dir f]
  (let [rels-path (if f
                    (unix-path (fs/unroll (file (fs/parent-file (file f)) "_rels" (str (.getName (file f)) ".rels"))))
                    (unix-path (file "_rels" ".rels"))) 
        rels-file (file dir rels-path)]
    (when (fs/exists? rels-file)
      {:stencil.model/path rels-path
       :source-file rels-file
       :parsed (parse rels-file)})))

(defn targets-by-type
  "Returns seq of paths from relations definition where relation type matches the predicate."
  [relations type-pred]
  (assert (map? (:parsed relations)))
  (for [v (vals (:parsed relations))
        :when (type-pred (:stencil.model/type v))]
    (:stencil.model/target v)))

(defn writer [relation-map]
  (assert (map? relation-map))
  (assert (every? string? (keys relation-map)) (str "Not all str: " (keys relation-map)))
  (->
   {:tag tag-relationships
    :content (for [[k v] relation-map]
               {:tag tag-relationship
                :attrs (cond-> {:Type (:stencil.model/type v), :Target (:stencil.model/target v), :Id k}
                         (:stencil.model/mode v) (assoc :TargetMode (:stencil.model/mode v)))})}
   ;; LibreOffice opens the generated document only when default xml namespace is the following:
   (with-meta {:clojure.data.xml/nss
               (pu/assoc pu/EMPTY "" "http://schemas.openxmlformats.org/package/2006/relationships")})
   (->xml-writer)))


(defn- map-rename-relation-ids [item id-rename]
  (-> item
      ;; Image relation ids are being renamed here.
      (update-some [:attrs ooxml/r-embed] id-rename)
      ;; Hyperlink relation ids are being renamed here
      (update-some [:attrs ooxml/r-id] id-rename)))


(defn xml-rename-relation-ids [id-rename xml-tree]
  (if (map? xml-tree)
    (-> xml-tree
        (map-rename-relation-ids id-rename)
        (update :content (partial map (partial xml-rename-relation-ids id-rename))))
    xml-tree))


;; generates a random relation id
(defn- ->relation-id [] (str (gensym "stencilRelId")))


(defn ids-rename [model fragment-name]
  (doall
   (for [[old-rel-id m] (-> model :main :relations :parsed (doto assert))
         :when (#{rel-type-image rel-type-hyperlink} (:stencil.model/type m))
         :let [new-id       (->relation-id)
               new-path     (if (= "External" (:stencil.model/mode m))
                              (:stencil.model/target m)
                              (str new-id "." (last (.split (str (:stencil.model/target m)) "\\."))))]]
     {:stencil.model/type       (:stencil.model/type m)
      :stencil.model/mode       (:stencil.model/mode m)
      :stencil.model/target     new-path
      :fragment-name fragment-name
      :new-id      new-id
      :old-id      old-rel-id
      :source-file (file (-> model :main :source-file file fs/parent-file) (:stencil.model/target m))
      :stencil.model/path       new-path})))

;; set of extra relations to be added after evaluating document
(def ^:dynamic *extra-files* nil)

(defmacro with-extra-files-context [body]
  `(binding [*extra-files* (atom #{})] ~body))

(defn add-extra-file! [m]
  (assert (:new-id m))
  (swap! *extra-files* conj m) m)

(defn model-assoc-extra-files [m fragment-names]
  (assert *extra-files*)
  (assert (set? fragment-names))
  (cond-> m
    ;; create a rels file for the current xml
    (and (seq @*extra-files*) (nil? (:stencil.model/path (:relations m))))
    (assoc-in [:relations :stencil.model/path]
              (unix-path (file (fs/parent-file (file (:stencil.model/path m)))
                          "_rels"
                          (str (.getName (file (:stencil.model/path m))) ".rels"))))

    ;; add relations if any
    (seq @*extra-files*)
    (update-in [:relations :parsed] (fnil into {})
                (for [relation @*extra-files*
                      :when (or (not (contains? relation :fragment-name))
                                (contains? fragment-names (:fragment-name relation)))]
                  [(:new-id relation) relation]))

    ;; relation file will be rendered instead of copied
    (seq @*extra-files*)
    (update-in [:relations] dissoc :source-file)))

(defn assoc-relation [model name type target]
  (-> model
      (assoc-in [:main :relations :parsed name]
                {:stencil.model/type type
                 :stencil.model/target target})
      (update-in [:main :relations] dissoc :source-file)))

(defn path-by-type [model rel-type]
  (assert (:relations model))
  (assert (string? rel-type))
  (some->> model :relations :parsed vals
           (find-first #(= rel-type (:stencil.model/type %)))
           :stencil.model/target))
