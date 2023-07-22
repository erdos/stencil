(ns stencil.util
  (:require [clojure.zip])
  (:import [io.github.erdos.stencil.exceptions ParsingException EvalException]))

(set! *warn-on-reflection* true)

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

(defn fixpt [f x] (let [fx (f x)] (if (= fx x) x (recur f fx))))
(defn zipper? [loc] (-> loc meta (contains? :zip/branch?)))
(defn iterations [f elem] (eduction (take-while some?) (iterate f elem)))

;; same as (first (filter pred xs))
(defn find-first [pred xs] (reduce (fn [_ x] (if (pred x) (reduced x))) nil xs))
(defn find-last [pred xs] (reduce (fn [a x] (if (pred x) x a)) nil xs))

(def xml-zip
  "Like clojure.zip/xml-zip but more flexible. Only maps are considered branches."
  (partial clojure.zip/zipper
           map?
           (comp seq :content)
           (fn [node children] (assoc node :content (some-> children vec)))))

(defn assoc-if-val [m k v]
  (if (some? v) (assoc m k v) m))

(defn suffixes [xs] (take-while seq (iterate next xs)))
(defn prefixes [xs] (take-while seq (iterate butlast xs)))

(defmacro fail [msg obj]
  (assert (string? msg))
  (assert (map? obj))
  `(throw (ex-info ~msg ~obj)))

(defn ->int [x]
  (cond (nil? x)    nil
        (string? x) (Integer/parseInt (str x))
        (number? x) (int x)
        :else       (fail "Unexpected type of input" {:type (:type x) :input x})))

(defn subs-last [^String s ^long n] (.substring s (- (.length s) n)))

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

(defn ^String string
  ([values] (apply str values))
  ([xform coll] (transduce xform (fn ([^Object s] (.toString s)) ([^StringBuilder b v] (.append b v))) (StringBuilder.) coll)))

(defmacro whitespace?? [c]
  `(case ~c (\tab \space \newline
                  \u00A0 \u2007 \u202F ;; non-breaking spaces
                  \u000B \u000C \u000D \u001C \u001D \u001E \u001F)
         true false))

(defn whitespace? [c] (whitespace?? c))

;; like clojure.string/trim but supports a wider range of whitespace characters
(defn ^String trim [^CharSequence s]
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
