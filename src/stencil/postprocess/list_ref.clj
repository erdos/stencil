(ns stencil.postprocess.list-ref)

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

(defn fix-list-dirty-refs [tree] tree)
