(ns stencil.postprocess.fragments
  "Inserts contents of fragments."
  (:import [stencil.types FragmentInvoke])
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.ooxml :as ooxml]
            [stencil.model :as model]
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

;; removes all left neighbors except for rPr, pPr nodes. stays at original loc.
(defn- remove-all-lefts [loc']
  (loop [loc loc']
    (if (zip/left loc)
      (recur (zip/next (zip/remove (zip/left loc))))
      (reduce zip/insert-left loc (filter tag-style (zip/lefts loc'))))))

(defn remove-all-rights
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

; (has-texts?

(defn- split-paragraphs [chunk-loc & insertable-paragraphs]
  (assert (= ooxml/t (:tag (zip/node (zip/up chunk-loc)))))
  (assert (= ooxml/r (:tag (zip/node (zip/up (zip/up chunk-loc))))))
  (assert (= ooxml/p (:tag (zip/node (zip/up (zip/up (zip/up chunk-loc)))))))



  (let [node-p?! (fn [x] (assert (= ooxml/p (:tag (zip/node x)))
                                (str "Not p: " (pr-str (zip/node x)))
                                ) x)
        node-t?! (fn [x] (assert (= ooxml/t (:tag (zip/node x)))
                                (str "Not t: " (pr-str (zip/node x)))) x)
        node-r?! (fn [x] (assert (= ooxml/r (:tag (zip/node x)))) x)

        p-left (-> chunk-loc
                   remove-all-rights
                   remove+up ; text
                   (node-t?!)
                   remove-all-rights
                   (node-t?!)
                   zip/up ; run
                   (node-r?!)
                   remove-all-rights
                   (node-r?!)
                   zip/up ; paragraph
                   (node-p?!)
                   zip/node)
        p-right (-> chunk-loc
                    remove-all-lefts
                    remove+up ; text
                    remove-all-lefts
                    zip/up ; run
                    remove-all-lefts
                    zip/up ; paragraph
                    zip/node)]
    ;; bukta!!!
    (assert (= ooxml/p (:tag p-left)) (str (pr-str p-left)))
    (assert (= ooxml/p (:tag p-right)))

    (-> chunk-loc
        (zip/up) ;; <t> tag: text
        (zip/up) ;; <r> tag: run
        (zip/up) ;; <p> tag: paragraph

        (cond-> (has-texts? p-left) (zip/insert-left p-left))
        (cond-> (has-texts? p-right) (zip/insert-right p-right))

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

;; Hatra van:
;;
;; - relaciok legyenek mergelve + a hivatkozott fajlok bemasolva a dokumentumba: kepek, fontok
;; - egy pelda doksi + dokumentacio bovebb leirassal
;; - fontosabb metodusokhoz egysegtesztek
