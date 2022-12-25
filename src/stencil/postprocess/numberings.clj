(ns stencil.postprocess.numberings
  (:require [stencil.util :refer :all]
            [stencil.ooxml :as ooxml]
            [stencil.model.numbering :as numbering]
            [stencil.log :as log]
            [clojure.zip :as zip]))

(defn- get-new-id [numbering-id trace]
  (log/debug "Getting new list for old {} trace {}" numbering-id trace)
  (if (empty? trace)
    numbering-id
    (numbering/copy-numbering! numbering-id)))

(defn- lookup [get-new-id element]
  (get-new-id (ooxml/val (:attrs element))
              (take-last (:stencil.cleanup/depth (:attrs element))
                         (:stencil.eval/trace (:attrs element)))))

(defn- fix-one [numbering lookup]
  (-> numbering
      (update :attrs dissoc :stencil.cleanup/depth :stencil.eval/trace)
      (update :attrs assoc ooxml/val (lookup numbering))))

(defn fix-numberings [xml-tree]
  (let [lookup (partial lookup (memoize get-new-id))]
    (dfs-walk-xml-node
     xml-tree
     (fn [e] (= ooxml/attr-numId (:tag e)))
     (fn [e] (zip/edit e fix-one lookup)))))

