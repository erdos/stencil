(ns stencil.postprocess.fragments
  "Inserts contents of fragments."
  (:import [stencil.types FragmentInvoke])
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer :all]))


(defn- remove+up
  "Removes current node and moves pointer to parent node."
  [loc]
  (let [|lefts| (count (zip/lefts loc))
        parent-loc (zip/up loc)]
    (->> (zip/make-node loc
                        (zip/node parent-loc)
                        (concat (take |lefts| (zip/children parent-loc))
                                (drop (inc |lefts|) (zip/children parent-loc))))
         (zip/replace parent-loc))))


;; returns nil iff it is not a styling element
(defn- tag-style [node] (#{ooxml/pPr ooxml/rPr} (:tag node)))


(defn- remove-all-lefts
  "Removes all left siblings. Stays at original location."
  [loc']
  (loop [loc loc']
    (if (zip/left loc)
      (recur (zip/next (zip/remove (zip/left loc))))
      (reduce zip/insert-left loc (filter tag-style (zip/lefts loc'))))))


(defn- remove-all-rights
  "Removes all right siblings. Stays at original location."
  [loc]
  (let [parent-loc (zip/up loc)
        |lefts| (inc (count (zip/lefts loc)))]
    (->> (zip/make-node loc (zip/node parent-loc) (take |lefts| (zip/children parent-loc)))
         (zip/replace parent-loc)
         (zip/down)
         (zip/rightmost))))


(defn- has-texts? [tree]
  (assert (= ooxml/p (:tag tree)))
  (not (empty?
        (for [run (:content tree)
              :when (map? run)
              :when (= ooxml/r (:tag run))
              text (:content run)
              :when (map? text)
              :when (= ooxml/t (:tag text))
              c (:content run)
              :when (string? c)
              :when (not-empty c)] c))))


(defn- node-p?! [x] (assert (= ooxml/p (:tag (zip/node x)))) x)
(defn- node-t?! [x] (assert (= ooxml/t (:tag (zip/node x)))) x)
(defn- node-r?! [x] (assert (= ooxml/r (:tag (zip/node x)))) x)

(defn- split-texts [chunk-loc & insertable-texts])

(defn- split-runs [chunk-loc & insertable-runs])

(defn- split-paragraphs [chunk-loc & insertable-paragraphs]
  (let [p-left (-> chunk-loc
                   (remove-all-rights)
                   (remove+up) ; text
                   (node-t?!)
                   (remove-all-rights)
                   (node-t?!)
                   (zip/up) ; run
                   (node-r?!)
                   (remove-all-rights)
                   (node-r?!)
                   (zip/up) ; paragraph
                   (node-p?!)
                   zip/node)
        p-right (-> chunk-loc
                    (remove-all-lefts)
                    (remove+up) ; text
                    (node-t?!)
                    (remove-all-lefts)
                    (zip/up) ; run
                    (node-r?!)
                    (remove-all-lefts)
                    (zip/up) ; paragraph
                    (node-p?!)
                    (zip/node))]
    (-> chunk-loc
        (zip/up) ;; <t> tag: text
        (zip/up) ;; <r> tag: run
        (zip/up) ;; <p> tag: paragraph

        (cond-> (has-texts? p-left) (zip/insert-left p-left))
        (cond-> (has-texts? p-right) (zip/insert-right p-right))

        (as-> * (reduce zip/insert-left * insertable-paragraphs))

        (zip/remove))))


(defn unpack-items [node-to-replace & insertable-nodes]
  (assert (zipper? node-to-replace))
  (assert (sequential? insertable-nodes))
  ;; XXX: split here!
  ;; TODO: split here: decide if paragraph or run or text shall be split!
  (apply split-paragraphs node-to-replace insertable-nodes))


(defn- unpack-fragment [chunk-loc]
  (assert (instance? FragmentInvoke (zip/node chunk-loc)))
  (let [chunk      (-> chunk-loc zip/node :result (doto (assert "result is missing")))
        tree-parts (-> chunk :frag-evaled-parts (doto (assert "Evaled parts is missing")))]
    (apply unpack-items chunk-loc tree-parts)))


(defn unpack-fragments
  "Walks the tree (Depth First) and evaluates FragmentInvoke objects."
  [xml-tree]
  (dfs-walk-xml-node xml-tree (partial instance? FragmentInvoke) unpack-fragment))
