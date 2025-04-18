(ns stencil.util
  (:require [clojure.zip])
  (:import [io.github.erdos.stencil.exceptions ParsingException EvalException]))

(set! *warn-on-reflection* true)

(def open-tag "{%")
(def close-tag "%}")

(defn stacks-difference-key
  "Removes suffixes of two lists where key-fn gives the same result."
  [key-fn stack1 stack2]
  (assert (ifn? key-fn))
  (let [cnt (count (take-while true?
                               (map (fn [a b] (= (key-fn a) (key-fn b)))
                                    (reverse stack1) (reverse stack2))))]
    [(take (- (count stack1) cnt) stack1)
     (take (- (count stack2) cnt) stack2)]))

(defn update-peek
  "Updates top element of a stack."
  [xs f & args]
  (assert (ifn? f))
  (conj (pop xs) (apply f (peek xs) args)))

(defn mod-stack-top-last
  "Updatest last element of top elem of stack."
  [stack f & args]
  (assert (list? stack) (str "Stack is not a list: " (pr-str stack)))
  (apply update-peek stack update-peek f args))

(defn mod-stack-top-conj
  "Conjoins an element to the top item of a stack."
  [stack & items]
  (update-peek stack into items))

(defn update-some [m path f]
  (or (some->> (get-in m path) f (assoc-in m path)) m))

(defn fixpt
  "Repeatedly calls (f x), (f (f x)), (f (f (f x))) etc until we get the same element with further function applications."
  [f x] (let [fx (f x)] (if (= fx x) x (recur f fx))))

(defn zipper? [loc] (-> loc meta (contains? :zip/branch?)))
(defn iterations
  "Returns an eduction of x, (f x) (f (f x)), ... while the value is not nil."
  [f x] (eduction (take-while some?) (iterate f x)))

;; same as (first (filter pred xs))
(defn find-first
  "Given a reducible coll, returns first element for which predicate is true. Returns nil when no such element is found."
  [pred coll] (reduce (fn [_ x] (when (pred x) (reduced x))) nil coll))

(defn find-last
  "Given a reducible coll, returns last element for which predicate is true. Returns nil when no such element is found."
  [pred coll] (reduce (fn [a x] (if (pred x) x a)) nil coll))

(def xml-zip
  "Like clojure.zip/xml-zip but more flexible. Only maps are considered branches."
  (partial clojure.zip/zipper
           map?
           (comp seq :content)
           (fn [node children] (assoc node :content (some-> children vec)))))

(defn assoc-some
  "Associate key k to value v in map m if v is not nil."
  [m k v] (if (some? v) (assoc m k v) m))

(defmacro fail [msg obj]
  (assert (string? msg))
  (assert (map? obj))
  `(throw (ex-info ~msg ~obj)))

(defn ->int [x]
  (cond (nil? x)    nil
        (string? x) (Integer/parseInt (str x))
        (number? x) (int x)
        :else       (fail "Unexpected type of input" {:type (:type x) :input x})))

(defn parsing-exception [expression message]
  (ParsingException/fromMessage (str expression) (str message)))

(defn eval-exception [message expression]
  (assert (string? message))
  (EvalException. message expression))

;; return xml zipper of location that matches predicate or nil
(defn find-first-in-tree [predicate tree-loc]
  (assert (ifn? predicate))
  (assert (zipper? tree-loc))
  (letfn [(coords-of-first [node]
            (loop [children (:content node)
                   index 0]
              (when-let [[c & cs] (not-empty children)]
                (if (predicate c)
                  [index]
                  (if-let [cf (coords-of-first c)]
                    (cons index cf)
                    (recur cs (inc index)))))))
          (nth-child [loc i]
            (loop [loc (clojure.zip/down loc), i i]
              (if (zero? i) loc (recur (clojure.zip/right loc) (dec i)))))]
    (if (predicate (clojure.zip/node tree-loc))
      tree-loc
      (when-let [coords (coords-of-first (clojure.zip/node tree-loc))]
        (reduce nth-child tree-loc coords)))))

(defn- dfs-walk-xml-node-1 [loc predicate edit-fn]
  (assert (zipper? loc))
  (loop [loc loc]
    (if (clojure.zip/end? loc)
      (clojure.zip/root loc)
      (if (predicate (clojure.zip/node loc))
        (recur (clojure.zip/next (edit-fn loc)))
        (recur (clojure.zip/next loc))))))

(defn dfs-walk-xml-node [xml-tree predicate edit-fn]
  (assert (fn? predicate))
  (assert (fn? edit-fn))
  (assert (map? xml-tree))
  (if-let [loc (find-first-in-tree predicate (xml-zip xml-tree))]
    (dfs-walk-xml-node-1 loc predicate edit-fn)
    xml-tree))

(defn dfs-walk-xml [xml-tree predicate edit-fn]
  (assert (fn? edit-fn))
  (dfs-walk-xml-node xml-tree predicate #(clojure.zip/edit % edit-fn)))

(defn unlazy-tree [xml-tree]
  (assert (map? xml-tree))
  (doto xml-tree
    (-> :content dorun)))

(defmacro when-pred [pred body]
  `(let [b# ~body]
     (when (~pred b#) b#)))

(defn string ^String [xform coll] (transduce xform (fn ([^Object s] (.toString s)) ([^StringBuilder b v] (.append b v))) (StringBuilder.) coll))

(defmacro whitespace?? [c]
  `(case ~c (\tab \space \newline
                  \u00A0 \u2007 \u202F ;; non-breaking spaces
                  \u000B \u000C \u000D \u001C \u001D \u001E \u001F)
         true false))

(defn whitespace? [c] (whitespace?? c))

;; like clojure.string/trim but supports a wider range of whitespace characters
(defn trim ^String [^CharSequence s]
  (loop [right-idx (.length s)]
    (if (zero? right-idx)
      ""
      (if (whitespace?? (.charAt s (dec right-idx)))
        (recur (dec right-idx))
        (loop [left-idx 0]
          (if (whitespace?? (.charAt s left-idx))
            (recur (inc left-idx))
            (.toString (.subSequence s left-idx right-idx))))))))

:OK
