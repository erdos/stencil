(ns stencil.model.numbering
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer [unlazy-tree ->int assoc-if-val find-first]]
            [stencil.model.common :refer [unix-path ->xml-writer]]))


(def ^:private rel-type-numbering
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering")

;; children of numbering xml definition as a vector in an atom.
(def ^:dynamic *numbering* nil)

(defn -initial-numbering-context [template-model]
  (let [xml-tree (-> template-model :main :stencil.model/numbering :parsed)]
    {:extra-elems (atom []) ;; elems added during evaluation in context
     :parsed      xml-tree}))

;; defines target context. changes to numberngs will be moved here.
(defmacro with-numbering-context [template-model body]
  `(binding [*numbering* (-initial-numbering-context ~template-model)]
     ;; TODO: update body by writing new entries from current numbering context
     (let [body# ~body]
       (if-let [extra-elems# (seq @(:extra-elems *numbering*))]
         (-> body#
             (update-in [:main :stencil.model/numbering] dissoc :source-file)
             (update-in [:main :stencil.model/numbering :parsed :content] conj extra-elems#)
             (update-in [:main :stencil.model/numbering]
                        (fn [nr#] (assoc nr# :result {:writer (->xml-writer (:parsed nr#))}))))
         body#))))

(defn- add-numbering-entry! [xml-element]
  ; (println :add-numbering-entry xml-element)
  (swap! (:extra-elems *numbering*) conj xml-element) nil)

;; cache atom is a map with {:num-id-rename {} :abstract-num-id-rename {}}
(defn copy-numbering [source-model cache-atom numbering-id]
  ;(println :XXXX numbering-id)
  (let [numbering-root         (-> source-model :main :stencil.model/numbering :parsed)
        id->numbering          (into {} (for [e (:content numbering-root)
                                              :when (= ooxml/tag-num (:tag e))]
                                          [(get-in e [:attrs ooxml/attr-numId]) e]))
        id->abstract-numbering (into {} (for [e (:content numbering-root)
                                              :when (= ooxml/tag-abstract-num (:tag e))]
                                          [(get-in e [:attrs ooxml/xml-abstract-num-id]) e]))
        copy-abstract-nring (fn [cache abstract-nr-id]
                              (if (contains? (:abstract-num-id-rename cache) abstract-nr-id)
                                cache
                                (let [elem   (id->abstract-numbering abstract-nr-id)
                                      new-id (name (gensym "snan"))]
                                  (add-numbering-entry! (assoc-in elem [:attrs ooxml/xml-abstract-num-id] new-id))
                                  (assoc-in cache [:abstract-num-id-rename abstract-nr-id] new-id))))
        copy-nring (fn [cache numbering-id]
                     (if (contains? (:num-id-rename cache) numbering-id)
                       cache
                       (let [elem   (id->numbering numbering-id)
                             new-id (name (gensym "snnn"))

                             ;; if numbering definition has an abstractNumId child then we need to also map that
                             abstract-id? (->> elem :content
                                               (find-first (fn [e] (= ooxml/xml-abstract-num-id (:tag e))))
                                               :attrs ooxml/val)
                             cache        (if abstract-id? (copy-abstract-nring cache abstract-id?) cache)
                             abstract-rename? (get-in cache [:abstract-num-id-rename abstract-id?])]
                         (-> elem
                             (assoc-in [:attrs ooxml/attr-numId] new-id)
                             (update :content (partial mapv (fn [c] (if (= ooxml/xml-abstract-num-id (:tag c))
                                                                      (assoc-in c [:attrs ooxml/val] abstract-rename?)
                                                                      c))))
                             (add-numbering-entry!))
                         (assoc-in cache [:num-id-rename numbering-id] new-id))))]
    (assert numbering-root)
    (-> cache-atom
        (swap! copy-nring numbering-id)
        (get :num-id-rename)
        (get numbering-id)
        (doto (->> (println :XXX/copy-numbering numbering-id)))
        )))

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
  (some-> (:parsed *numbering*)
          (get-id-style-xml id lvl)
          (xml-lvl-parse)))
