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
   \| :or})

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
  (let [until (quotation-marks (first characters))
        sb    (new StringBuilder)]
    (loop [[c & cs] (next characters)]
      (cond (nil? c) (throw (ex-info "String parse error"
                                     {:reason "Unexpected end of stream"}))
            (= c until)        [(.toString sb) cs]
            (= c (first "\\")) (do (.append sb (first cs)) (recur (next cs)))
            :else              (do (.append sb c) (recur cs))))))

(defn read-number
  "Reads a number literal from a sequence. Returns a tuple of read
   number (Double or Long) and the sequence of remaining characters."
  [characters]
  (let [content (string (take-while (set "1234567890._")) characters)
        content (.replaceAll content "_" "")
        number  (if (some #{\.} content)
                  (Double/parseDouble content)
                  (Long/parseLong     content))]
    [number (drop (count content) characters)]))

(defn tokenize
  "Returns a sequence of tokens for an input string"
  [original-string]
  (loop [[first-char & next-chars :as characters] (str original-string)
         tokens []]
    (cond
      (empty? characters)
      tokens

      (whitespace? first-char)
      (recur next-chars tokens)

      (contains? #{\, \;} first-char)
      (recur next-chars (conj tokens :comma))

      (contains? ops2 [first-char (first next-chars)])
      (recur (next next-chars) (conj tokens (ops2 [first-char (first next-chars)])))

      (= \- first-char)
      (recur next-chars (conj tokens :minus))

      (contains? ops first-char)
      (recur next-chars (conj tokens (ops first-char)))

      (contains? digits first-char)
      (let [[n tail] (read-number characters)]
        (recur tail (conj tokens n)))

      (quotation-marks first-char)
      (let [[s tail] (read-string-literal characters)]
        (recur tail (conj tokens s)))

      :else
      (let [content (string (take-while identifier) characters)]
        (assert (not-empty content))
        (recur (drop (count content) characters)
               (conj tokens (symbol content)))))))

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
(println :!!!!)
;(println :> (parse "2"))
; (println :> (parse "2 + 3"))
;(println :> (parse "2 * 3"))
; (println :! (parse "2*3"))
; (println (parse "2*(-3)"))

;(println :!! (tokenize "2*-(3)"))
(println :!! (parse "!!a"))
;(System/exit -1)
;(assert false)