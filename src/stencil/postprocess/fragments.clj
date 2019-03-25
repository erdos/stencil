(ns stencil.postprocess.fragments
  "Calls deref on delayed values in an XML tree."
  (:import [stencil.types FragmentInvoke])
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.ooxml :as ooxml]
            [stencil.model :as model]
            [stencil.util :refer :all]))

;; removes current node and moves pointer to parent node.
(defn- remove+up [loc] (if (zip/left loc) (zip/up (zip/remove loc)) (zip/remove loc)))

;; returns node if it is a styling element
(defn- tag-style [node] (when (contains? #{ooxml/pPr ooxml/rPr} (:tag node)) node))

;; removes all left neighbors except for rPr, pPr nodes. stays at original loc.
(defn- remove-all-lefts [loc']
  (loop [loc loc']
    (if (zip/left loc)
      (recur (zip/next (zip/remove (zip/left loc))))
      (reduce zip/insert-left loc (keep tag-style (zip/lefts loc'))))))

;; removes all right neighbors. stays at original loc.
(defn remove-all-rights [loc]
  (if (zip/right loc) (recur (zip/remove (zip/right loc))) loc))

(defn- split-paragraphs [chunk-loc & insertable-paragraphs]
  (assert (= ooxml/t (:tag (zip/node (zip/up chunk-loc)))))
  (assert (= ooxml/r (:tag (zip/node (zip/up (zip/up chunk-loc))))))
  (assert (= ooxml/p (:tag (zip/node (zip/up (zip/up (zip/up chunk-loc)))))))

  (let [p-left (-> chunk-loc
                   remove-all-rights
                   remove+up ; t
                   remove-all-rights
                   zip/up ; r
                   remove-all-rights
                   zip/up ; p
                   zip/node)
        p-right (-> chunk-loc
                    remove-all-lefts
                    remove+up ; t
                    remove-all-lefts
                    zip/up ; r
                    remove-all-lefts
                    zip/up ; p
                    zip/node)]
    (assert (= ooxml/p (:tag p-left)) (str (pr-str p-left)))
    (assert (= ooxml/p (:tag p-right)))

    (-> chunk-loc
        (zip/up) ;; <t> tag: text
        (zip/up) ;; <r> tag: run
        (zip/up) ;; <p> tag: paragraph

        (zip/insert-left p-left)
        (zip/insert-right p-right)

        (as-> * (reduce zip/insert-left * insertable-paragraphs))

        (zip/remove))))

(defn- unpack-fragment [chunk-loc]
  (assert (instance? FragmentInvoke (zip/node chunk-loc)))
  (let [chunk-name (-> chunk-loc zip/node :name (doto (assert "name is missing")))
        local-data (-> chunk-loc zip/node :data (doto (assert "data is missing")))
        chunk      (model/insert-fragment! chunk-name local-data)
        tree-parts (:frag-evaled-parts chunk)]
    (assert chunk "Chunk not found!")
    (assert (sequential? tree-parts) "Tree pasrts must be a sequence!")

    (apply split-paragraphs chunk-loc tree-parts)))

(defn unpack-fragments
  "Walks the tree (Depth First) and evaluates FragmentInvoke objects."
  [xml-tree]
  (dfs-walk-xml-node xml-tree (partial instance? FragmentInvoke) unpack-fragment))
