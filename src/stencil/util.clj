(ns stencil.util
  (:require [clojure.zip])
  (:import [io.github.erdos.stencil.exceptions EvalException ParsingException]))

(set! *warn-on-reflection* true)

(defn stacks-difference-key
  "Removes prefixes of two lists where key-fn gives the same result."
  [key-fn stack1 stack2]
  (assert (ifn? key-fn))
  (let [cnt (count (take-while true?
                               (map (fn [a b] (= (key-fn a) (key-fn b)))
                                    (reverse stack1) (reverse stack2))))]
    [(take (- (count stack1) cnt) stack1)
     (take (- (count stack2) cnt) stack2)]))

(def stacks-difference
  "Removes comomn prefix of two lists."
  (partial stacks-difference-key identity))

(defn mod-stack-top-last
  "Egy stack legfelso elemenek legutolso elemet modositja.
   Ha nincs elem, IllegalStateException kivetelt dob."
  [stack f & args]
  (assert (list? stack) (str "Stack is not a list: " (pr-str stack)))
  (assert (ifn? f))
  (conj (rest stack)
        (conj (pop (first stack))
              (apply f (peek (first stack)) args))))

(defn mod-stack-top-conj
  "Egy stack legfelso elemehez hozzafuz egy elemet"
  [stack & items]
  (conj (rest stack) (apply conj (first stack) items)))

(defn update-peek
  "Egy stack legfelso elemet modositja."
  [xs f & args]
  (assert (ifn? f))
  (conj (pop xs) (apply f (peek xs) args)))

(defn update-some [m path f]
  (if-some [x (get-in m path)]
    (if-some [fx (f x)]
      (assoc-in m path fx)
      m)
    m))

(defn fixpt [f x] (let [fx (f x)] (if (= fx x) x (recur f fx))))
(defn zipper? [loc] (-> loc meta (contains? :zip/branch?)))
(defn iterations [f xs] (take-while some? (iterate f xs)))
(defn find-first [pred xs] (first (filter pred xs)))
(defn find-last [pred xs] (last (filter pred xs)))

(defn keepv [f xs] (vec (keep f xs)))
(defn concatv [& xs] (vec (apply concat xs)))

(def xml-zip
  "Like clojure.zip/xml-zip but more flexible. Only maps are considered branches."
  (partial clojure.zip/zipper
           map?
           (comp seq :content)
           (fn [node children] (assoc node :content (and children (apply vector children))))))

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

(def print-trace? false)

(defmacro trace [msg & details]
  (assert (string? msg) "Log message must be a string")
  `(when print-trace?
     (println (format ~msg ~@(for [d details] `(pr-str ~d))))))



(defn parsing-exception [expression message]
  (ParsingException/fromMessage (str expression) (str message)))

(defn eval-exception-missing [expression]
  (EvalException/fromMissingValue (str expression)))

(defn dfs-walk-xml-node [xml-tree predicate edit-fn]
  (assert (map? xml-tree))
  (assert (fn? predicate))
  (assert (fn? edit-fn))
  (loop [loc (xml-zip xml-tree)]
    (if (clojure.zip/end? loc)
      (clojure.zip/root loc)
      (if (predicate (clojure.zip/node loc))
        (recur (clojure.zip/next (edit-fn loc)))
        (recur (clojure.zip/next loc))))))

(defn dfs-walk-xml [xml-tree predicate edit-fn]
  (assert (fn? edit-fn))
  (dfs-walk-xml-node xml-tree predicate #(clojure.zip/edit % edit-fn)))


:OK
