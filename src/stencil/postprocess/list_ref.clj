(ns stencil.postprocess.list-ref
  (:require [stencil.util :refer :all]
            [stencil.ooxml :as ooxml]
            [stencil.model.numbering :as numbering]
            [clojure.zip :as zip]))

(set! *warn-on-reflection* true)

;; see val for numbering:
;; http://officeopenxml.com/WPnumbering-numFmt.php
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

(defn- num-to-roman [n]
  (assert (integer? n))
  (assert (pos? n))
  (loop [buf [], n n]
    (if (zero? n)
      (apply str buf)
      (let [[value romnum] (some #(if (>= n (first %)) %) roman-digits)]
        (recur (conj buf romnum) (- n value))))))

(defmethod render-number "upperRoman" [_ number]
  (num-to-roman number))

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

;; reference: https://c-rex.net/projects/samples/ooxml/e1/Part4/OOXML_P4_DOCX_REFREF_topic_ID0ESRL1.html#topic_ID0ESRL1

;; ".%1/%2." -> ".%1/%2."
;; "%1/%2."  -> "%1/%2"
;; "%1/%2"   -> "%1/%2"
(defn pattern-rm-prefix-if-no-suffix [^String pattern]
  (if-not (.startsWith pattern "%")
    pattern
    (.substring pattern 0 (+ 2 (.lastIndexOf pattern "%")))))

;; full context is joining of patterns.
(defn- render-list-full-context [styles levels]
  (let [pattern (let [patterns (mapv :lvl-text (take (count levels) styles))]
                  (loop [i (dec (count patterns))]
                    (if (neg? i)
                      (apply str patterns)
                      (let [cleaned (pattern-rm-prefix-if-no-suffix (nth patterns i))]
                        (if (= (nth patterns i) cleaned)
                          (recur (dec i))
                          (str (apply str (take i patterns)) cleaned (apply str (drop (inc i) patterns))))))))]
    (reduce-kv (fn [pattern idx item] (.replace (str pattern) (str "%" (inc idx)) (str item)))
               pattern
               (mapv (fn [style level] (render-number (:num-fmt style) (+ (:start style) level -1)))
                     styles levels))))

(defn- render-list-one [styles levels]
  (let [pattern (str (:lvl-text (nth styles (dec (count levels)))))
        pattern (pattern-rm-prefix-if-no-suffix pattern)]
    (reduce-kv (fn [pattern idx item] (.replace (str pattern) (str "%" (inc idx)) (str item)))
               pattern
               (mapv (fn [style level] (render-number (:num-fmt style) (+ (:start style) level -1)))
                     styles levels))))

(defn- render-list-relative [styles levels current-stack]
  ;; TODO!
  (render-list-one styles levels))

;; returns "below" or "above" or nil
(defn- render-list-position [styles levels current-stack]
  nil)

(defn render-list [styles levels flags current-stack]
  (assert (sequential? styles))
  (assert (sequential? levels))
  (assert (sequential? current-stack))
  (assert (<= (count levels) (count styles)))
  (assert (set? flags))
  (-> (cond (:w flags) (render-list-full-context styles levels)
            (:r flags) (render-list-relative styles levels current-stack)
            (:n flags) (render-list-one styles levels))
      (cond-> (:p flags) (-> (some-> (str " ")) (str (render-list-position styles levels current-stack))))))

(defn instr-text-ref [node]
  (assert (not (zipper? node)))
  (when (map? node)
    (when (= ooxml/tag-instr-text (:tag node))
      (first (:content node)))))

;; returns node if it is an fldChar node
(defn fld-char [node]
  (when (map? node)
    (when (= ooxml/fld-char (:tag node))
      node)))

;; (find-elem zipper :tag "ala")
;; (find-elem zipper :attr "x" "1")
(defn find-elem [tree prop & [a b]]
  (assert (zipper? tree))
  (assert (keyword? a))
  (let [items (take-while (comp complement #{(zip/node tree)} zip/node) (iterate zip/next tree))]
    (case prop
      :tag  (find-first (comp #{a} :tag zip/node) items)
      :attr (find-first (comp #{b} a :attrs zip/node) items))))

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

;; loc points to the run which contains <fldChar w:fldCharType="begin"/>
(defn replace-ref-text [loc]
  (assert (zipper? loc))
  ;; loc points
  (assert (-> loc zip/node :tag name (= "r")))

  ;; go right, find <fldChar fldCharType="end"/>
  nil

  )

(defn parse-instr-text [^String s]
  (assert (string? s))
  (let [[type id & flags] (vec (.split (.trim s) "\\s\\\\?+"))]
    (when (= "REF" type)
      {:id id
       :flags (set (map keyword flags))})))

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
    (dfs-walk-xml-node
     @xml-tree
     instr-text-ref ;; if it is a ref node
     (fn [loc]
       ;; go right, find text node between seaprate and end.
       ;; we can replace text with a rendered value.
       (let [text (instr-text-ref (zip/node loc))]
         (->
          (some-> loc
             (zip/up) ;; run
             (zip/right) ;; run
             (->> (when-pred #(find-elem % :attr ooxml/fld-char-type "separate")))
             (zip/right)
             ((fn [loc]
                (when-let [txt (find-elem loc :tag ooxml/t)]
                  (let [current-text (-> txt zip/node :content first)
                        parsed-ref   (parse-instr-text text)]
                    (when-let [{:keys [num-id ilvl stack]} (get @bookmark->meta (:id parsed-ref))]
                      (let [definitions (doall (for [i (range (inc ilvl))]
                                                 (numbering/style-def-for num-id i)))
                            current-stack (some->> (iterations zip/up loc)
                                                   (find-first (comp #{ooxml/p} :tag zip/node) )
                                                   (child-of-tag "pPr")
                                                   (child-of-tag "numPr")
                                                   (zip/node)
                                                   (::enumeration)
                                                   (:stack))
                            replacement (render-list definitions stack #{} (or current-stack ()))]
                        (-> txt
                            (zip/edit assoc :content [replacement])
                            (zip/up))))))))
             (zip/right)
             (->> (when-pred #(find-elem % :attr ooxml/fld-char-type "end"))))
          (or loc)))))))
