(ns stencil.postprocess.list-ref
  (:require [stencil.util :refer :all]
            [stencil.ooxml :as ooxml]
            [stencil.model.numbering :as numbering]
            [clojure.tools.logging :as log]
            [clojure.zip :as zip]))

(set! *warn-on-reflection* true)

;; see val for numbering:
;; http://officeopenxml.com/WPnumbering-numFmt.php
;; http://www.datypic.com/sc/ooxml/t-w_ST_NumberFormat.html
;; TODO: cardinalText, ordinal, ordinalText, ...
(defmulti render-number (fn [style number] style))
(defmethod render-number :default [_ nr] (str nr))

(def ^:private roman-digits
  [[1000 "M"] [900 "CM"]
   [500  "D"] [400 "CD"]
   [100  "C"] [90  "XC"]
   [50   "L"] [40  "XL"]
   [10   "X"] [9   "IX"]
   [5    "V"] [4   "IV"]
   [1    "I"]])

(defmethod render-number "upperRoman" [_ number]
  (assert (pos? number))
  (loop [buf [], n number]
    (if (zero? n)
      (apply str buf)
      (let [[value romnum] (some #(if (>= n (first %)) %) roman-digits)]
        (recur (conj buf romnum) (- n value))))))

(defmethod render-number "lowerRoman" [_ number]
  (.toLowerCase (str (render-number "upperRoman" number))))

(defmethod render-number "decimal" [_ number] (str (int number)))
(defmethod render-number "decimalZero" [_ number]
  (let [result (str (int number))]
    (if (= 1 (count result))
      (str "0" result)
      result)))

(defmethod render-number "decimalEnclosedParen" [_ number]
  (str "(" (int number) ")"))

(defmethod render-number "upperLetter" [_ number]
  (assert (pos? number))
  (let [abc "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        len (count abc)]
    (loop [number number
           out    ""]
      (if (zero? number)
        out
        (let [q (quot (dec number) len)
              r (rem (dec number) len)]
          (recur q (str (nth abc r) out)))))))

(defmethod render-number "lowerLetter" [_ number]
  (.toLowerCase (str (render-number "upperLetter" number))))

(defmethod render-number "chicago" [_ number]
  (nth (for [i (next (range)), c "*†‡§‖#"]
         (clojure.string/join (repeat i c)))
       (dec number)))

(defmethod render-number "none" [_ number] "")
(defmethod render-number "bullet" [_ number] "")

;; reference: https://c-rex.net/projects/samples/ooxml/e1/Part4/OOXML_P4_DOCX_REFREF_topic_ID0ESRL1.html#topic_ID0ESRL1

;; ".%1/%2." -> ".%1/%2."
;; "%1/%2."  -> "%1/%2"
;; "%1/%2"   -> "%1/%2"
(defn pattern-rm-prefix-if-no-suffix [^String pattern]
  (if-not (.startsWith pattern "%")
    pattern
    (.substring pattern 0 (+ 2 (.lastIndexOf pattern "%")))))

;; full context is joining of patterns.
(defn- render-list-full-context [styles levels drop-start]
  (assert (not (neg? drop-start)))
  (let [pattern (let [patterns (mapv :lvl-text (take (count levels) styles))]
                  (->>
                   (loop [i (dec (count patterns))]
                     (if (neg? i)
                       patterns
                       (let [cleaned (pattern-rm-prefix-if-no-suffix (nth patterns i))]
                         (if (= (nth patterns i) cleaned)
                           (recur (dec i))
                           (concat (take i patterns) [cleaned] (drop (inc i) patterns))))))
                   (drop (min drop-start (dec (count levels))))
                   (apply str)))]
    (reduce-kv (fn [pattern idx item] (.replace (str pattern) (str "%" (inc idx)) (str item)))
               pattern
               (mapv (fn [style level] (render-number (:num-fmt style) (+ (:start style) level -1)))
                     styles (reverse levels)))))

(defn- render-list-one [styles levels]
  (let [pattern (str (:lvl-text (nth styles (dec (count levels)))))
        pattern (pattern-rm-prefix-if-no-suffix pattern)]
    (reduce-kv (fn [pattern idx item] (.replace (str pattern) (str "%" (inc idx)) (str item)))
               pattern
               (mapv (fn [style level] (render-number (:num-fmt style) (+ (:start style) level -1)))
                     styles (reverse levels)))))

(defn- render-list-relative [styles levels current-stack]
  (let [common-suffix (count (take-while true? (map = (reverse levels) (reverse current-stack))))]
    (render-list-full-context styles levels common-suffix)))

;; returns "below" or "above"
(defn- render-list-position [bookmark parsed-ref]
  (assert (:order bookmark))
  (assert (:order parsed-ref))
  (if (< (:order bookmark) (:order parsed-ref)) "above" "below"))

;; returns string concatenation of text contents from the seq of runs.
(defn- render-bookmark-content [runs]
  (apply str
         (for [r runs
               :when (= ooxml/r (:tag r))
               c (:content r)
               :when (map? c)
               :when (= ooxml/t (:tag c))
               t (:content c)]
           t)))

(defn render-list [styles {:keys [stack] :as bookmark} {:keys [flags] :as parsed-ref} current-stack]
  (assert (sequential? styles))
  (assert (map? bookmark))
  (assert (sequential? current-stack))
  (assert (<= (count stack) (count styles)))
  (assert (set? flags))
  (-> (cond (:w flags) (render-list-full-context styles stack 0)
            (:r flags) (render-list-relative styles stack current-stack)
            (:n flags) (render-list-one styles stack)
            (not (:p flags)) (render-bookmark-content (:runs bookmark)))
      (cond-> (:p flags) (-> (some-> (str " ")) (str (render-list-position bookmark parsed-ref))))))

;; lazy seq of all zippers in the subtree walked by preorder DFS graph traversal
(defn- descendants [tree]
  (assert (zipper? tree))
  (cons tree
        ((fn f [tree depth]
           (if-let [d (zip/down tree)]
             (cons d (lazy-seq (f d (inc depth))))
             (loop [tree tree, depth depth]
               (when (pos? depth)
                 (if-let [r (zip/right tree)]
                   (cons r (lazy-seq (f r depth)))
                   (recur (zip/up tree) (dec depth)))))))
         tree 0)))

;; Walks the tree (zipper) with DFS and returns the first node for given tag or attribute.
(defn- find-elem [tree prop & [a b]]
  (assert (zipper? tree))
  (assert (keyword? a))
  (case prop
    :tag  (find-first (comp #{a} :tag zip/node) (descendants tree))
    :attr (find-first (comp #{b} a :attrs zip/node) (descendants tree))))

(defn- parse-num-pr [node]
  (assert (= ooxml/num-pr (:tag node)))
  (reduce (fn [m node]
            (case (some-> node :tag name)
              "ilvl" (assoc m :ilvl (-> node :attrs ooxml/val ->int)) ;; level, starting from 0
              "numId" (assoc m :num-id (-> node :attrs ooxml/val))
              m)) {} (:content node)))

(defn- child-of-tag [tag-name loc]
  (assert (zipper? loc))
  (assert (string? tag-name))
  (find-first (comp (fn [elem] (and (map? elem) (some-> elem :tag name #{tag-name}))) zip/node)
              (iterations zip/right (zip/down loc))))

(defn parse-instr-text [^String s]
  (assert (string? s))
  (let [[type id & flags] (vec (.split (.trim s) "\\s\\\\?+"))]
    (when (= "REF" type)
      {:id id
       :flags (set (map keyword flags))})))

(defn- fill-crossref-content [loc parsed-ref bookmark]
  (assert (zipper? loc))
  (assert (map? parsed-ref))
  (when-let [txt (find-elem loc :tag ooxml/t)]
    (let [old-content (-> txt zip/node :content first)]
      (if bookmark
        (let [definitions (doall (for [i (range (inc (:ilvl bookmark)))]
                                   (numbering/style-def-for (:num-id bookmark) i)))
              current-stack (some->> (iterations zip/up loc)
                                     (find-first (comp #{ooxml/p} :tag zip/node))
                                     (child-of-tag "pPr")
                                     (child-of-tag "numPr")
                                     (zip/node)
                                     (::enumeration)
                                     (:stack))
              replacement (render-list definitions bookmark parsed-ref (or current-stack ()))]
          (log/debug "Replacing" old-content "with" replacement "in" (:id parsed-ref))
          (-> txt
              (zip/edit assoc :content [replacement])
              (zip/up)))
        (do (log/warn "Reference source not found. Previous content:" old-content "id:" (:id parsed-ref))
            ;(zip/edit txt assoc :content ["Error; Reference source not found."])
            nil)))))

;; adds ::enumeration key to all numPr elements
;; adds ::instruction key to all instrText elements
(defn- enrich-dirty-refs-meta [xml-tree]
  (let [order     (volatile! 0)
        nr->stack (volatile! {})]
    (dfs-walk-xml
     xml-tree
     (fn [node] (and (map? node) (#{ooxml/num-pr ooxml/tag-instr-text} (:tag node))))
     (fn [node]
       (condp = (:tag node)
         ooxml/tag-instr-text
         (assoc node ::instruction {:order (vswap! order inc)})

         ooxml/num-pr
         (let [{:keys [ilvl num-id]} (parse-num-pr node)]
           (vswap! nr->stack
                   update
                   num-id
                   (fnil
                    (fn [stack length]
                      (cond (< (inc length) (count stack))
                            (update-peek (next stack) inc)

                            (> (inc length) (count stack))
                            (conj stack 1)

                            :else
                            (update-peek stack inc)))
                    ())
                   ilvl)
           (assoc node ::enumeration
                  {:ilvl ilvl
                   :num-id num-id
                   :order (vswap! order inc)
                   :stack (get @nr->stack num-id)})))))))

;; Given a location of a bookmark start node, find the <r> nodes until the corresponding <bookmarkEnd>
(defn- get-bookmarked-runs [zipper]
  (assert (= ooxml/bookmark-start (:tag (zip/node zipper))))
  (let [bookmark-id (-> zipper zip/node :attrs ooxml/name)]
    (->> zipper
         (iterations zip/right)
         (next)
         (take-while (comp (fn [n]
                             (or (-> n :tag (not= ooxml/bookmark-end))
                                 (-> n :attrs ooxml/name (not= bookmark-id))))
                           zip/node))
         (filter (comp (fn [n] (= (:tag n) ooxml/r)) zip/node))
         (map zip/node)
         (doall))))

;; Produces map of Bookmark id (REF string) to metadata map. Map contains values from under
;; the ::enumeration key of numbering node.
(defn- get-bookmark-meta [xml-tree]
  (let [bookmark->meta (volatile! {})]
    (dfs-walk-xml-node
     xml-tree
     (fn [node] (and (map? node) (= (:tag node) ooxml/bookmark-start)))
     (fn [zipper]
       (let [bookmark-id (-> zipper zip/node :attrs ooxml/name)]
         (some->> zipper
                  (zip/up)
                  (child-of-tag "pPr")
                  (child-of-tag "numPr")
                  (zip/node)
                  (::enumeration)
                  (merge {:runs (get-bookmarked-runs zipper)})
                  (vswap! bookmark->meta assoc bookmark-id))
         zipper)))
    @bookmark->meta))

;; if node is an instrText then return the string in it
(defn- instr-text-ref [node]
  (when (and (map? node) (= ooxml/tag-instr-text (:tag node)))
    (first (:content node))))

(defn- rerender-refs [xml-tree bookmark->meta]
  (assert (map? bookmark->meta))
  (dfs-walk-xml-node
   xml-tree
   instr-text-ref ;; if it is a ref node
   (fn [loc]
     ;; go right, find text node between separate and end.
     ;; we can replace text with a rendered value.
     (let [node (zip/node loc)
           text (instr-text-ref node)
           parsed-ref (merge (parse-instr-text text) (::instruction node))]
       (->
        (some-> (when parsed-ref loc)
                (zip/up) ;; run
                (->> (iterations zip/right)
                     (find-first #(find-elem % :attr ooxml/fld-char-type "separate")))
                (zip/right)
                (fill-crossref-content parsed-ref (bookmark->meta (:id parsed-ref)))
                (zip/right)
                (->> (when-pred #(find-elem % :attr ooxml/fld-char-type "end"))))
        (or loc))))))

(defn fix-list-dirty-refs [xml-tree]
  (let [xml-tree (enrich-dirty-refs-meta xml-tree)
        bookmark-meta (get-bookmark-meta xml-tree)]
    (rerender-refs xml-tree bookmark-meta)))
