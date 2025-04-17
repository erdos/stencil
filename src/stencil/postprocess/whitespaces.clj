(ns stencil.postprocess.whitespaces
  "Logics of handling whitespace characters in OOXML text runs.
 
   The code visits all <w/t> elements and modifies content where necessary:
   - If content starts or ends with whitespace, add a space=preserve attribute.
   - Replace \n characters with <w/br /> elements.
   - Replace \t characters with <w/tab /> elements."
  (:require [clojure.string :refer [starts-with? ends-with? index-of]]
            [clojure.zip :as zip]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer [dfs-walk-xml-node zipper?]]))

;; Returns smallest index of c1 or c2 in s or nil when not found.
(defn- first-index-of [s c1 c2]
  (assert (string? s))
  (let [idx1 (index-of s c1)
        idx2 (index-of s c2)]
    (if (and idx1 idx2)
      (min idx1 idx2)
      (or idx1 idx2))))

(defn- should-fix? [element]
  (when (and (map? element)
             (= ooxml/t (:tag element))
             (not-empty (:content element)))
    (or (starts-with? (first (:content element)) " ")
        (ends-with? (last (:content element)) " ")
        (some #(first-index-of (str %) \newline \tab) (:content element)))))

(defn- multi-replace [loc items]
  (assert (zipper? loc))
  (assert (not-empty items))
  (reduce (comp zip/right zip/insert-right) (zip/replace loc (first items)) (next items)))

;; Returns a lazy seq of substrings split by \t or \n, separators included.
(defn- split-str [s]
  (assert (string? s))
  (if-let [idx (first-index-of s \newline \tab)]
    (if (zero? idx)
      (list*                (subs s 0 1)           (lazy-seq (split-str (subs s 1))))
      (list* (subs s 0 idx) (subs s idx (inc idx)) (lazy-seq (split-str (subs s (inc idx))))))
    (if (empty? s) [] (list s))))

(defn- str->element [item]
  (cond (= "\n" item)
        ,,,{:tag ooxml/br}
        (= "\t" item)
        ,,,{:tag ooxml/tab}
        (or (starts-with? item " ") (ends-with? item " "))
        ,,,{:tag ooxml/t :content [item] :attrs {ooxml/space "preserve"}}
        :else
        ,,,{:tag ooxml/t :content [item]}))

(defn- fix-elem-node [loc]
  (->> (:content (zip/node loc))
       (apply str)
       (split-str)
       (map str->element)
       (multi-replace loc)))

(defn fix-whitespaces [xml-tree] (dfs-walk-xml-node xml-tree should-fix? fix-elem-node))
 
