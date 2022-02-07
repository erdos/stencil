(ns stencil.postprocess.whitespaces
  (:require [clojure.string :refer [includes? starts-with? ends-with? index-of]]
            [clojure.zip :as zip]
            [stencil.ooxml :as ooxml]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

(defn- should-fix?
  "We only fix <t> tags where the enclosed string starts or ends with whitespace."
  [element]
  (boolean
   (when (and (map? element)
              (= ooxml/t (:tag element))
              (seq (:content element)))
     (or (starts-with? (first (:content element)) " ")
         (ends-with? (last (:content element)) " ")
         (some #(includes? % "\n") (:content element))))))

(defn- multi-replace [loc items]
  (assert (zipper? loc))
  (assert (not-empty items))
  (reduce (comp zip/right zip/insert-right) (zip/replace loc (first items)) (next items)))

(defn- lines-of [s]
  (if-let [idx (index-of s "\n")]
    (if (zero? idx)
      (cons :newline (lazy-seq (lines-of (subs s 1))))
      (list* (subs s 0 idx) :newline (lazy-seq (lines-of (subs s (inc idx))))))
    (if (empty? s) [] (list s))))

(defn item->elem [item]
  (cond (= :newline item)
        ,,,{:tag ooxml/br}
        (or (starts-with? item " ") (ends-with? item " "))
        ,,,{:tag ooxml/t :content [item] :attrs {ooxml/space "preserve"}}
        :else
        ,,,{:tag ooxml/t :content [item]}))

(defn- fix-elem-node [loc]
  (->> (apply str (:content (zip/node loc)))
       (lines-of)
       (map item->elem)
       (multi-replace loc)))

(defn fix-whitespaces [xml-tree] (dfs-walk-xml-node xml-tree should-fix? fix-elem-node))
