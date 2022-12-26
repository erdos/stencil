(ns stencil.model.style
  (:import [java.io File])
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [stencil.ooxml :as ooxml]
            [stencil.model.common :refer :all]
            [stencil.util :refer :all]))


(set! *warn-on-reflection* true)

(def rel-type
  "Relationship type of style definitions in _rels/.rels file."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles")

;; style definitions of the main document
(def ^:dynamic *current-styles* nil)


;; throws error when not invoked from inside fragment context
(defmacro expect-fragment-context! [& bodies] `(do (assert *current-styles*) ~@bodies))


(defn- parse
  "Returns a map where key is style id and value is style definition."
  [style-file]
  (assert style-file)
  (with-open [r (io/input-stream (io/file style-file))]
    (into (sorted-map)
          (for [d (:content (xml/parse r))
                :when (map? d)
                :when (= ooxml/style (:tag d))]
            [(ooxml/style-id (:attrs d)) d]))))

(defn file-writer [template]
  (expect-fragment-context!
   (let [original-style-file (:source-file (:style (:main template)))
         _ (assert (.exists ^File original-style-file))
         extended-tree (with-open [r (io/input-stream original-style-file)]
                         (let [tree (xml/parse r)
                               all-ids (set (keep (comp ooxml/style-id :attrs) (:content tree)))
                               insertable (vals (apply dissoc @*current-styles* all-ids))]
                           (update tree :content (comp vec concat) insertable)))]
     {:writer (->xml-writer extended-tree)})))


(defn- -insert-style!
  "Returns possibly new style id."
  [style-definition]
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
  "Returns a map of all style definitions where key is style id from style-defs and value is new style id."
  [style-defs]
  (assert (map? style-defs) (str "Not map: " (pr-str style-defs) (type style-defs)))
  (assert (every? string? (keys style-defs)))
  (reduce (fn [m [id style]]
            (let [id2 (-insert-style! style)]
              (if (= id id2) m (assoc m id id2))))
          {} style-defs))


(defn xml-rename-style-ids
  "Recursively renames occurrences of old style ids to new ids based on rename map."
  [style-id-renames xml-tree]
  (if (map? xml-tree)
    (if (-> xml-tree :tag name (.endsWith "Style"))
      (update-some xml-tree [:attrs ooxml/val] style-id-renames)
      (update xml-tree :content (partial mapv (partial xml-rename-style-ids style-id-renames))))
    xml-tree))


(defn- main-style-item [^File dir main-document main-document-rels]
  (when-let [main-style (find-first #(= rel-type (:stencil.model/type %))
                              (vals (:parsed main-document-rels)))]
    (let [main-style-file (io/file (.getParentFile (io/file main-document))
                                   (:stencil.model/target main-style))
          main-style-abs  (io/file dir main-style-file)]
      {:stencil.model/path (unix-path main-style-file)
       :source-file        main-style-abs
       :parsed             (parse main-style-abs)})))


(defn assoc-style [model dir]
  (->> (main-style-item dir (:stencil.model/path model) (:relations model))
       (assoc-if-val model :style)))
