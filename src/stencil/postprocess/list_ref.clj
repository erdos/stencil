(ns stencil.postprocess.list-ref
  (:require [stencil.util :refer :all]
            [stencil.ooxml :as ooxml]
            ; [stencil.model :as model]
            [clojure.zip :as zip]))

(set! *warn-on-reflection* true)

;; see val for numbering:
;; http://officeopenxml.com/WPnumbering-numFmt.php
(defmulti render-number (fn [style number] style))
(defmethod render-number :default [_ nr] (str nr))

(defmethod render-number "lowerRoman" [_ number]
  (assert false))

(defmethod render-number "upperRoman" [_ number]
  (.toUpperCase (str (render-number "lowerRoman" number))))

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

;; style: decimal
(defn render-list [styles levels flags]
  (assert (sequential? styles))
  (assert (sequential? levels))
  (assert (<= (count levels) (count styles)))
  (assert (set? flags))
  (reduce-kv (fn [pattern idx item] (.replace (str pattern) (str "%" (inc idx)) (str item)))
             (str (:lvl-text (nth styles (dec (count levels)))))
             (mapv (fn [style level] (render-number (:num-fmt style) (+ (:start style) level -1))) styles levels)))

(defn node-instr-ref? [node]
  ;; returns true iff node is a paragraph number reference
  nil)

(defn instr-text-ref [node]
  (when (map? node)
    (when (= :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/instrText
             (:tag node))
      (first (:contents node)))))

;; returns node if it is an fldChar node
(defn fld-char [node]
  (when (map? node)
    (when (= ooxml/fld-char (:tag node))
      node)))

(defn- find-first-in-tree [pred tree]
  (assert (zipper? tree))
  (assert (fn? pred))
  (find-first (comp pred zip/node) (take-while (complement zip/end?) (iterate zip/next tree))))

(defn parse-num-pr [node]
  (assert (= ooxml/num-pr (:tag node)))
  (reduce (fn [m node]
            (case (some-> node :tag name)
              "ilvl" (assoc m :ilvl (-> node :attrs ooxml/val ->int)) ;; level, starting from 0
              "numId" (assoc m :num-id (-> node :attrs ooxml/val))
              m)) {} (:content node)))

(defn- find-first-child
  "Returns zipper of first child where predicate holds for the node or nil when not found."
  [pred loc]
  (assert (ifn? pred))
  (assert (zipper? loc))
  (find-first (comp pred zip/node) (take-while some? (iterations zip/right (zip/down loc)))))

(defn- tag-matches? [tag elem] (and (map? elem) (some-> elem :tag name #{tag})))

(defn- child-of-tag [tag-name loc]
  (assert (zipper? loc))
  (assert (string? tag-name))
  (find-first-child (partial tag-matches? tag-name) loc))

(defn fix-list-dirty-refs [xml-tree]
  (let [xml-tree (atom xml-tree)
        bookmark->meta (volatile! {})]

    ;; step 1: add meta data to all numPr elements
    (let [nr->stack (volatile! {})]
      (swap! xml-tree
             dfs-walk-xml
             (fn [node] (and (map? node) (= (:tag node) ooxml/num-pr)))
             (fn [node]
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
                         :stack (get @nr->stack num-id)})))))



    ;; step 2:
    ;;
    ;; - for all bookmark node
    ;; - find their position and stack snapshot
    ;; - save it to global atom
    (dfs-walk-xml-node
     @xml-tree
     (fn [node] (and (map? node) (= (:tag node) ooxml/bookmark-start)))
     (fn [zipper]
       (let [bookmark-id (->(zip/node zipper) :attrs ooxml/name)]
         (some->> zipper
                  (zip/up)
                  (child-of-tag "pPr")
                  (child-of-tag "numPr")
                  (zip/node)
                  (::enumeration)
                  (vswap! bookmark->meta assoc bookmark-id))
         zipper)))
    (println "Bookmark meta: " @bookmark->meta)

    ;; step 3:
    ;;
    ;; - for all reference
    ;; - find bookmark meta in global atom
    ;; - re-calculate rendered cross-ref data


    ;; TODO: make sure numbering relationship file is added (also for fragments!)
    ;; TODO: make sure we have access to numbering definition
    ;; TODO: read numbering definition, transform it to acceptable form
    ;; TODO: do the rendering based on style def

    @xml-tree)

  #_
  (let [bookmark-nodes (atom {})]
    (dfs-walk-xml-node xml-tree
                       (fn [node] (and (map? node) (= (:tag node) ooxml/bookmark-start)))
                       (fn [zipper]
                         (let [id (-> zipper zip/node :attrs ooxml/name)
                               nr-id (->> zipper
                                          zip/up
                                          (find-first-in-tree (fn [node] (= tag-num-id (:tag node))))
                                          zip/node
                                          :attrs
                                          attr-val)

                               ;; go left from node, find
                               nr-idx nil]
                           (swap! bookmark-nodes assoc id [nr-id nr-idx]))
                         zipper))
    (println "!!!")
    (println @bookmark-nodes)
    xml-tree)
  xml-tree
  )
