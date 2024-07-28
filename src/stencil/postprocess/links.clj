(ns stencil.postprocess.links
  (:require [clojure.zip :as zip]
            [stencil.functions :refer [def-stencil-fn]]
            [stencil.log :as log]
            [stencil.ooxml :as ooxml]
            [stencil.model.relations :as relations]
            [stencil.types :refer [ControlMarker]]
            [stencil.util :refer [fail find-first iterations dfs-walk-xml-node]]))

(set! *warn-on-reflection* true)

;; Tells if the reference of an adjacent hyperlink node should be replaced in postprocess step.
(defrecord ReplaceLink [relation] ControlMarker)

(defn- update-link [link-node, ^ReplaceLink data]
  (assert (= ooxml/hyperlink (:tag link-node)))
  (assert (instance? ReplaceLink data))
  (let [current-rel (-> link-node :attrs ooxml/r-id)
        new-val     (-> data .relation)]
    (assert new-val)
    (log/debug "Replacing hyperlink relation {} by {}" current-rel new-val)
    (assoc-in link-node [:attrs ooxml/r-id] new-val)))

(defn- replace-link [marker-loc]
  (if-let [link-loc (->> (zip/remove marker-loc)
                         (iterations zip/prev)
                         (find-first (comp #{ooxml/hyperlink} :tag zip/node)))]
    (zip/edit link-loc update-link (zip/node marker-loc))
    (fail "Did not find hyperlink to replace. The location of target link must precede the replaceLink() function call location." {})))

(defn replace-links [xml-tree]
  (dfs-walk-xml-node
   xml-tree
   (partial instance? ReplaceLink)
   replace-link))

;; This duplicates both stencil.postprocess.image/->relation-id,
;; and stencil.model.relations/->relation-id
;; TODO: maybe make stencil.model.relations/->relation-id public
(defn- ->relation-id [] (str (gensym "srel")))

(defn- link-url->relation [url]
  (let [new-rel  (->relation-id)]
    {:new-id               new-rel
     :stencil.model/type   relations/rel-type-hyperlink
     :stencil.model/target url
     :stencil.model/mode   "External"}))

;; replaces the nearest link's URK with the parameter value
(def-stencil-fn "replaceLink"
  "Replaces the link URL in the hyperlink preceding this expression.
   It should be placed immediately after the link we want to modify."
  [url]
  (let [new-relation (link-url->relation (str url))]
    (relations/add-extra-file! new-relation)
    (->ReplaceLink (:new-id new-relation))))