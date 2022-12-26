(ns stencil.model.numbering
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer [unlazy-tree ->int find-first assoc-if-val]]
            [stencil.model.common :refer [unix-path]]))


(def ^:private rel-type-numbering
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering")

;; swap an atom here!
(def ^:dynamic *numbering* nil)


(defn- find-node [tree predicate]
  (when (map? tree)
    (if (predicate tree)
      tree
      (some #(find-node % predicate) (:content tree)))))


(defn- find-lvl [tree level]
  (find-node tree
             (fn [node]
               (and (map? node)
                    (= (:tag node) ooxml/tag-lvl)
                    (= (str level) (-> node :attrs ooxml/attr-ilvl str))))))


(defn- get-id-style-xml [tree id level]
  (assert (integer? level))
  (assert (string? id))
  (assert (map? tree) (str "Not a map: " (pr-str (type tree))))
  (let [def1 (find-node tree
                       (fn [node]
                         (and (map? node)
                              (= (:tag node) ooxml/tag-num)
                              (= id (-> node :attrs ooxml/attr-numId)))))]
    (or (find-lvl def1 level) ;; find in override

        ;; find abstract definition
        (let [abstract-id (-> (find-node def1 (comp #{ooxml/xml-abstract-num-id} :tag)) :attrs ooxml/val)
              abstract (find-node tree
                                  (fn [node]
                                    (and (= (:tag node) ooxml/tag-abstract-num)
                                         (= abstract-id (-> node :attrs ooxml/xml-abstract-num-id)))))]
          (find-lvl abstract level)))))


(defn- xml-lvl-parse [tree]
  (assert (= ooxml/tag-lvl (:tag tree)))
  (letfn [(node-attr [tag] (-> tree (find-node (comp #{tag} name :tag)) :attrs ooxml/val))]
    {:lvl-text (node-attr "lvlText")
     :num-fmt  (node-attr "numFmt")
     :start    (->int (node-attr "start"))}))


(defn prepare-numbering-xml [xml-tree]
  (unlazy-tree xml-tree))


(defn- parse [numbering-file]
  (assert numbering-file)
  (with-open [r (io/input-stream (io/file numbering-file))]
    (let [tree (xml/parse r)]
      (prepare-numbering-xml tree))))


(defn- main-numbering [dir main-document main-document-rels]
  (when-let [main-numbering-path
             (some #(when (= rel-type-numbering (:stencil.model/type %))
                      (unix-path (io/file (.getParentFile (io/file main-document))
                                       (:stencil.model/target %))))
                   (vals (:parsed main-document-rels)))]
    {:stencil.model/path       main-numbering-path
     :source-file              (io/file dir main-numbering-path)
     :parsed                   (parse (io/file dir main-numbering-path))}))

(defn assoc-numbering [model dir]
  (->> (main-numbering dir (:stencil.model/path model) (:relations model))
       (assoc-if-val model :stencil.model/numbering)))

(defn style-def-for [id lvl]
  (assert (string? id))
  (assert (integer? lvl))
  (some-> (:parsed @*numbering*)
          (get-id-style-xml id lvl)
          (xml-lvl-parse)))


(defn- tag-lvl-start-override [lvl start]
  {:tag ooxml/lvl-override
   :attrs {ooxml/attr-ilvl lvl}
   :content [{:tag ooxml/start-override :attrs {ooxml/val start}}]})


(defn copy-numbering!
  "Creates a copy of the numbering definition an returns the new id for it."
  [old-id]
  (let [old-elem (find-first (fn [e] (-> e :attrs ooxml/attr-numId (= old-id)))
                             (:content (:parsed @*numbering*)))
        abstract-num-id (some (fn [e]
                                (when (= ooxml/xml-abstract-num-id (:tag e))
                                  (-> e :attrs ooxml/val)))
                              (:content old-elem))
        max-num-id (apply max (keep (comp ->int ooxml/attr-numId :attrs)
                                    (:content (:parsed @*numbering*))))
        new-id (str (inc max-num-id))
        new-elem (assoc-in old-elem [:attrs ooxml/attr-numId] new-id)
        new-elem (update new-elem :content concat
                         (for [abstract (:content (:parsed @*numbering*))
                               :when (= abstract-num-id (-> abstract :attrs ooxml/xml-abstract-num-id))
                               lvl (:content abstract)
                               :when (= (:tag lvl) ooxml/tag-lvl)
                               start (:content lvl)
                               :when (= "start" (name (:tag start)))]
                           (tag-lvl-start-override (-> lvl :attrs ooxml/attr-ilvl) (-> start :attrs ooxml/val))))]
    (assert old-elem)
    (swap! *numbering* update :parsed update :content concat [new-elem])
    (swap! *numbering* dissoc :source-file)
    (swap! *numbering* (fn [numbering]
                         (assoc numbering :result {:writer (stencil.model.common/->xml-writer (:parsed numbering))})))
    new-id))
