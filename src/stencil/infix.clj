(ns stencil.infix
  "Parsing and evaluating infix algebraic expressions.

  https://en.wikipedia.org/wiki/Shunting-yard_algorithm"
  (:require [stencil.util :refer [fail update-peek ->int string whitespace?]]
            [stencil.log :as log]
            [stencil.functions :refer [call-fn]]
            [stencil.grammar :as grammar]))

(set! *warn-on-reflection* true)

(def ^:dynamic ^:private *calc-vars* {})

(def ops
  {\+ :plus
   \- :minus
   \* :times
   \/ :divide
   \% :mod
   \^ :power
   \( :open
   \) :close
   \[ :open-bracket
   \] :close-bracket
   \! :not
   \= :eq
   \< :lt
   \> :gt
   \& :and
   \| :or
   \, :comma \; :comma
   })

(def ops2 {[\> \=] :gte
           [\< \=] :lte
           [\! \=] :neq
           [\= \=] :eq
           [\& \&] :and
           [\| \|] :or})

(def digits
  "Number literals start with these characters"
  (set "1234567890"))

(def identifier
  "Characters found in an identifier"
  (set "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM_.1234567890"))

(def ^:private quotation-marks
  {\" \"   ;; programmer quotes
   \' \'   ;; programmer quotes
   \“ \”   ;; english double quotes
   \‘ \’   ;; english single quotes
   \’ \’   ;; hungarian single quotes (felidezojel)
   \„ \”}) ;; hungarian double quotes (macskakorom)

(defn read-string-literal
  "Reads a string literal from a sequence.
   Returns a tuple.
   - First elem is read string literal.
   - Second elem is seq of remaining characters."
  [characters]
  (when-let [until (quotation-marks (first characters))]
    (let [sb    (new StringBuilder)]
      (loop [[c & cs] (next characters)]
        (cond (nil? c) (throw (ex-info "String parse error"
                                      {:reason "Unexpected end of stream"}))
              (= c until)        [(.toString sb) cs]
              (= c (first "\\")) (do (.append sb (first cs)) (recur (next cs)))
              :else              (do (.append sb c) (recur cs)))))))

(defn read-number
  "Reads a number literal from a sequence. Returns a tuple of read
   number (Double or Long) and the sequence of remaining characters."
  [characters]
  (when (contains? digits (first characters))
    (let [content (string (take-while (set "1234567890._")) characters)
          content (.replaceAll content "_" "")
          number  (if (some #{\.} content)
                    (Double/parseDouble content)
                    (Long/parseLong     content))]
      [number (drop (count content) characters)])))

(defn- read-ops2 [chars]
  (when-let [op (get ops2 [(first chars) (second chars)])]
    [op (nnext chars)]))

(defn- read-ops1 [chars]
  (when-let [op (get ops (first chars))]
    [op (next chars)]))

(defn- read-iden [characters]
  (when-let [content (not-empty (string (take-while identifier) characters))]
    [(symbol content) (drop (count content) characters)]))

(def token-readers (some-fn read-number read-string-literal read-iden read-ops2 read-ops1))

(defn tokenize
  "Returns a sequence of tokens for an input string"
  [text]
  (when-let [text (seq (drop-while (comp whitespace? char) text))]
    (if-let [[token tail] (token-readers text)]
      (cons token (lazy-seq (tokenize tail)))
      (throw (ex-info "Unexpected endof string" {:text text})))))

(defmulti eval-tree (fn [tree] (if (sequential? tree) (first tree) (type tree))))

(defmethod eval-tree java.lang.Number [tree] tree)
(defmethod eval-tree String [s] s)
(defmethod eval-tree clojure.lang.Symbol [s] (get-in *calc-vars* (vec (.split (name s) "\\."))))

(defmethod eval-tree :eq [[_ a b]] (= (eval-tree a) (eval-tree b)))
(defmethod eval-tree :neq [[_ a b]] (not= (eval-tree a) (eval-tree b)))
(defmethod eval-tree :plus [[_ a b]]
  (let [a (eval-tree a) b (eval-tree b)]
    (if (or (string? a) (string? b))
      (str a b)
      (+ a b))))
(defmethod eval-tree :minus [[_ a b :as expr]]
  (if (= 2 (count expr))
    (- (eval-tree a))
    (- (eval-tree a) (eval-tree b))))
(defmethod eval-tree :times [[_ a b]] (* (eval-tree a) (eval-tree b)))
(defmethod eval-tree :divide [[_ a b]] (with-precision 8 (/ (eval-tree a) (eval-tree b))))

(defmethod eval-tree :or [[_ a b]] (or (eval-tree a) (eval-tree b)))
(defmethod eval-tree :and [[_ a b]] (and (eval-tree a) (eval-tree b)))
(defmethod eval-tree :mod [[_ a b]] (mod (eval-tree a) (eval-tree b)))
(defmethod eval-tree :not [[_ a]] (not (eval-tree a)))

(defmethod eval-tree :gte [[_ a b]] (>= (eval-tree a) (eval-tree b)))
(defmethod eval-tree :lte [[_ a b]] (<= (eval-tree a) (eval-tree b)))
(defmethod eval-tree :gt [[_ a b]] (> (eval-tree a) (eval-tree b)))
(defmethod eval-tree :lt [[_ a b]] (< (eval-tree a) (eval-tree b)))

(defmethod eval-tree :get [[_ m & path]]
  (reduce (fn [b a]
            (cond (sequential? b) (when (number? a) (get b (->int a)))
                  (string? b)     (when (number? a) (get b (->int a)))
                  (instance? java.util.List b) (when (number? a) (.get ^java.util.List b (->int a)))
                  :else           (get b (str a)))) 
          (eval-tree m) (map eval-tree path)))

(defmethod eval-tree :fncall [[_ f & args]]
;  (println :!!! (::functions *calc-vars*))
  (if-let [f (get (::functions *calc-vars*) (name f))]
    (apply f args)
    (throw (ex-info "No such fn" {}))))

(defn eval-rpn
  ([bindings default-function tree]
   (assert (ifn? default-function))
   (eval-rpn (assoc bindings ::functions default-function) tree))
  ([bindings tree]
   (assert (map? bindings))
   (binding [*calc-vars* bindings]
     (eval-tree tree))))

(def parse (comp (partial grammar/runlang grammar/testlang) tokenize))

:OK

;(println :>>> (tokenize "24 + 434"))
;(println :>>> (parse "24 + 434"))

;(println (tokenize "2*(-3)"))
;(println :!!!!)
;(println :> (parse "2"))
; (println :> (parse "2 + 3"))
;(println :> (parse "2 * 3"))
; (println :! (parse "2*3"))
; (println (parse "2*(-3)"))

;(println :!! (tokenize "2*-(3)"))
;(println :!! (parse "!!a"))
;(println :!! (parse "a | b"))
;(System/exit -1)
;(assert false)