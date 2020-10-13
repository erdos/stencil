(ns stencil.model.relations
  (:require [clojure.data.xml :as xml]
            [clojure.data.xml.pu-map :as pu]
            [clojure.java.io :as io :refer [file]]
            [stencil.util :refer :all]
            [stencil.model.common :refer :all]))

(def tag-relationships
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationships)

(def tag-relationship
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationship)


(defn parse [rel-file]
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
