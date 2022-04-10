(ns stencil.postprocess.whitespaces
  (:require [clojure.string :refer [includes? starts-with? ends-with? index-of]]
            [clojure.zip :as zip]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer :all]))

(defn- should-fix? [element]
  (when (and (map? element)
             (= ooxml/t (:tag element))
             (not-empty (:content element)))
    (or (starts-with? (first (:content element)) " ")
        (ends-with? (last (:content element)) " ")
        (some #(includes? % "\n") (:content element)))))

(defn- multi-replace [loc items]
  (assert (zipper? loc))
  (assert (not-empty items))
  (reduce (comp zip/right zip/insert-right) (zip/replace loc (first items)) (next items)))

;; (defn- lines-of [s] (enumeration-seq (java.util.StringTokenizer. s "\n" true)))
;; (defn- lines-of [s] (remove #{""} (interpose "\n" (clojure.string/split s "\n" -1))))

(defn- lines-of [s]
  (if-let [idx (index-of s "\n")]
    (if (zero? idx)
      (cons "\n" (lazy-seq (lines-of (subs s 1))))
      (list* (subs s 0 idx) "\n" (lazy-seq (lines-of (subs s (inc idx))))))
    (if (empty? s) [] (list s))))

(defn- item->elem [item]
  (cond (= "\n" item)
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
