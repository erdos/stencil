(ns stencil.model.numbering
  (:import [java.io File])
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io :refer [file]]
            [clojure.zip :as zip]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer :all]
            [stencil.model.common :refer :all]))


(def ^:private rel-type-numbering
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering")

(def tag-num
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/num)

(def tag-lvl
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/lvl)

(def xml-abstract-num-id
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/abstractNumId)

(def tag-abstract-num
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/abstractNum)

(def attr-numId
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/numId)

(def attr-ilvl
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/ilvl)

(def ^:dynamic *numbering* nil)

(defn- find-first-in-tree [pred tree]
  (assert (zipper? tree))
  (assert (fn? pred))
  (find-first (comp pred zip/node) (take-while (complement zip/end?) (iterate zip/next tree))))

(defn- find-first-child
  "Returns zipper of first child where predicate holds for the node or nil when not found."
  [pred loc]
  (assert (ifn? pred))
  (assert (zipper? loc))
  (find-first (comp pred zip/node) (take-while some? (iterations zip/right (zip/down loc)))))

(defn- tag-matches? [tag elem] (and (map? elem) (some-> elem :tag name #{tag})))

(defn- child-of-tag [tag-name loc]
  (assert (zipper? loc))
  (assert (string? tag-name))
  (find-first-child (partial tag-matches? tag-name) loc))

(defn- find-node [tree predicate]
  (when (map? tree)
    (if (predicate tree)
      tree
      (some #(find-node % predicate) (:content tree)))))

(defn find-lvl [tree level]
  (find-node tree
             (fn [node]
               (and (map? node)
                    (= (:tag node) tag-lvl)
                    (= (str level) (-> node :attrs attr-ilvl str))))))

(defn- get-id-style-xml [tree id level]
  (assert (integer? level))
  (assert (string? id))
  (assert (map? tree) (str "Not a map: " (pr-str (type tree))))
  (let [def1 (find-node tree
                       (fn [node]
                         (and (map? node)
                              (= (:tag node) tag-num)
                              (= id (-> node :attrs attr-numId)))))]
    (or (find-lvl def1 level) ;; find in override

        ;; find abstract definition
        (let [abstract-id (-> (find-node def1 (fn [node] (= (:tag node) xml-abstract-num-id ))) :attrs ooxml/val)
              abstract (find-node tree
                                  (fn [node]
                                    (and (= (:tag node) tag-abstract-num)
                                         (= abstract-id (-> node :attrs xml-abstract-num-id)))))]
          (find-lvl abstract level)))))

(defn- xml-lvl-parse [tree]
  {:lvl-text (-> (find-node tree (fn [node] (-> node :tag name #{"lvlText"}))) :attrs ooxml/val)
   :num-fmt  (-> (find-node tree (fn [node] (-> node :tag name #{"numFmt"}))) :attrs ooxml/val)
   :start    (-> (find-node tree (fn [node] (-> node :tag name #{"start"}))) :attrs ooxml/val ->int)})

(defn- parse [numbering-file]
  (assert numbering-file)
  (with-open [r (io/input-stream (file numbering-file))]
    (let [tree (xml/parse r)]
      tree)))


(defn main-numbering [dir main-document main-document-rels]
  (when-let [main-numbering-path
             (some #(when (= rel-type-numbering (:stencil.model/type %))
                      (unix-path (file (.getParentFile (file main-document))
                                       (:stencil.model/target %))))
                   (vals (:parsed main-document-rels)))]
    {:stencil.model/path       main-numbering-path
     :source-file              (io/file dir main-numbering-path)
     :parsed                   (parse (file dir main-numbering-path))}))

(defn style-def-for [id lvl]
  (assert (string? id))
  (assert (integer? lvl))
  (-> (:parsed *numbering*)
      (get-id-style-xml id lvl)
      (xml-lvl-parse)))
