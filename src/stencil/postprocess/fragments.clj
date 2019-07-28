(ns stencil.postprocess.fragments
  "Inserts contents of fragments."
  (:import [stencil.types FragmentInvoke])
  (:require [clojure.zip :as zip]
            [clojure.data.xml :as xml]
            [stencil.types :refer :all]
            [stencil.ooxml :as ooxml]
            [stencil.functions :refer [call-fn]]
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


(defn- split-texts [chunk-loc & insertable-runs]
  (fail "Not implemented!" {}))


(defn- split-runs [chunk-loc & insertable-runs]
  (assert (seq insertable-runs))
  (node-t?! (zip/up chunk-loc))
  (let [lefts (zip/lefts chunk-loc)
        rights (zip/rights chunk-loc)

        t    (zip/node (zip/up chunk-loc))
        r    (zip/node (zip/up (zip/up chunk-loc)))

        ;;  t elems
        lefts1 (remove (comp #{ooxml/rPr} :tag) (zip/lefts (zip/up chunk-loc)))
        rights1 (zip/rights (zip/up chunk-loc))

        ;; style of run that is split
        style (some #(when (= ooxml/rPr (:tag %)) %) (:content r))

        ->t (fn [xs] {:tag ooxml/t :content (vec xs)})
        ->run (fn [cts] (assoc r :content (vec (cons style cts))))]
    (assert (= ooxml/t (:tag t)))
    (assert (= ooxml/r (:tag r)))
    (-> chunk-loc
        (zip/up) ;; t
        (zip/up) ;; r

        (cond-> (seq lefts1) (zip/insert-left (->run lefts1)))
        (cond-> (seq lefts) (zip/insert-left (->run [(->t lefts)])))

        (cond-> (seq rights1) (zip/insert-right (->run rights1)))
        (cond-> (seq rights) (zip/insert-right (->run [(->t rights)])))

        (as-> * (reduce zip/insert-right * (reverse insertable-runs)))

        (zip/remove))))


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
  (assert (control? (zip/node node-to-replace)))
  (assert (sequential? insertable-nodes))
  (cond
    (= ooxml/r (:tag (first insertable-nodes)))
    (apply split-runs node-to-replace insertable-nodes)

    (= ooxml/t (:tag (first insertable-nodes)))
    (apply split-texts node-to-replace insertable-nodes)

    :else
    (apply split-paragraphs node-to-replace insertable-nodes)))


(defn- unpack-fragment [chunk-loc]
  (assert (instance? FragmentInvoke (zip/node chunk-loc)))
  (let [chunk      (-> chunk-loc zip/node :result (doto (assert "result is missing")))
        tree-parts (-> chunk :frag-evaled-parts (doto (assert "Evaled parts is missing")))]
    (apply split-paragraphs chunk-loc tree-parts)))


(defn unpack-fragments
  "Walks the tree (Depth First) and evaluates FragmentInvoke objects."
  [xml-tree]
  (dfs-walk-xml-node xml-tree (partial instance? FragmentInvoke) unpack-fragment))

;; custom XML content
(defmethod call-fn "xml" [_ content]
  (assert (string? content))
  (let [content (:content (xml/parse-str (str "<a>" content "</a>")))]
    (->FragmentInvoke {:frag-evaled-parts content})))
