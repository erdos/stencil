(ns stencil.infix
  "Parsing and evaluating infix algebraic expressions.

  https://en.wikipedia.org/wiki/Shunting-yard_algorithm"
  (:require [stencil.util :refer [fail update-peek ->int string]]
            [stencil.log :as log]
            [stencil.functions :refer [call-fn]]))

(set! *warn-on-reflection* true)

(def ^:dynamic ^:private *calc-vars* {})

(defrecord FnCall [fn-name])

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

(def operation-tokens
  "Operator precedences.

   source: http://en.cppreference.com/w/cpp/language/operator_precedence
  "
  {:open  -999
   ;;;:close -998
   :comma -998
   :open-bracket -999

   :or -21
   :and -20

   :eq -10 :neq -10,

   :lt -9 :gt -9 :lte -9 :gte -9

   :plus 2 :minus 2
   :times 3 :divide 4
   :power 5
   :not 6
   :neg 7})

(defn- precedence [token]
  (get operation-tokens token))

(defn- associativity [token]
  (if (#{:power :not :neg} token) :right :left))

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

(defn- read-comma [chars]
  (when (contains? #{\, \;} (first chars))
    [:comma (next chars)]))

(defn- read-ops2 [chars]
  (when-let [op (get ops2 [(first chars) (second chars)] )]
    [op (nnext chars)]))

(defn- read-ops1 [chars]
  (when-let [op (get ops (first chars))]
    [op (next chars)]))

(defn- read-iden [characters]
  (when-let [content (not-empty (string (take-while identifier) characters))]
    (let [tail (drop-while #{\space \tab} (drop (count content) characters))]
      (if (= \( (first tail))
        [(->FnCall content) (next tail)]
        [(symbol content) tail]))))

(def token-readers
  (some-fn read-comma
           read-number
           read-string-literal
           read-iden           
           read-ops2
           read-ops1))

(defn- tokenize'
  "Returns a sequence of tokens for an input string"
  [text]
  (when-let [text (seq (drop-while #(Character/isWhitespace (char %)) text))]
    (if-let [[token tail] (token-readers text)]
      (cons token (lazy-seq (tokenize' tail)))
      (throw (ex-info "Unexpected endof string" {:text text})))))

(defn tokenize [text]
  ;; replace :minus by :neg where it is negation instead of subtraction based on context
  (->> (tokenize' text)
       (reductions (fn [previous current] (if (and (= :minus current) (not= previous :close) (not= previous :close-bracket) (keyword? previous)) :neg current)) ::SENTINEL)
       (next)))

;; throws ExceptionInfo when token sequence has invalid elems
(defn- validate-tokens [tokens]
  (cond
    (some true? (map #(and (or (symbol? %1) (number? %1) (#{:close} %1))
                           (or (symbol? %2) (number? %2) (#{:open} %2)))
                     tokens (next tokens)))
    (throw (ex-info "Could not parse!" {}))

    :else
    tokens))

(defn tokens->rpn
  "Classic Shunting-Yard Algorithm extension to handle vararg fn calls."
  [tokens]
  (loop [[e0 & next-expr :as expr]      tokens ;; bemeneti token lista
         opstack   ()     ;; stack of Shunting-Yard Algorithm
         result    []     ;; Vector of output tokens


         parentheses 0 ;; count of open parentheses
         ;; on a function call we save function name here
         functions ()]
    (cond
      (neg? parentheses)
      (throw (ex-info "Parentheses are not balanced!" {}))

      (empty? expr)
      (if (zero? parentheses)
        (into result (remove #{:open}) opstack)
        (throw (ex-info "Too many open parentheses!" {})))

      (number? e0)
      (recur next-expr opstack (conj result e0) parentheses functions)

      (symbol? e0)
      (recur next-expr opstack (conj result e0) parentheses functions)

      (string? e0)
      (recur next-expr opstack (conj result e0) parentheses functions)

      (= :open e0)
      (recur next-expr (conj opstack :open) result (inc parentheses) (conj functions nil))

      (= :open-bracket e0)
      (recur next-expr (conj opstack :open-bracket) result (inc parentheses) functions)

      (instance? FnCall e0)
      (recur next-expr (conj opstack :open) result
             (inc parentheses)
             (conj functions {:fn (:fn-name  e0)
                              :args (if (= :close (first next-expr)) 0 1)}))
      ;; (recur next-expr (conj opstack :fncall) result (conj functions {:fn e0}))

      (= :close-bracket e0)
      (let [[popped-ops [_ & keep-ops]]
            (split-with (partial not= :open-bracket) opstack)]
        (recur next-expr
               keep-ops
               (into result (concat popped-ops [:get]))
               (dec parentheses)
               functions))

      (= :close e0)
      (let [[popped-ops [_ & keep-ops]]
            (split-with (partial not= :open) opstack)]
        (recur next-expr
               keep-ops
               (into result
                     (concat
                      (remove #{:comma} popped-ops)
                      (some-> functions first vector)))
               (dec parentheses)
               (next functions)))

      (empty? next-expr) ;; current is operator but without an operand
      (throw (ex-info "Missing operand!" {}))

      :else ;; operator
      (let [[popped-ops keep-ops]
            (split-with #(if (= :left (associativity e0))
                           (<= (precedence e0) (precedence %))
                           (< (precedence e0) (precedence %))) opstack)]
        (recur next-expr
               (conj keep-ops e0)
               (into result (remove #{:open :comma}) popped-ops)
               parentheses
               (if (= :comma e0)
                 (if (first functions)
                   (update-peek functions update :args inc)
                   (throw (ex-info "Unexpected ',' character!" {})))
                 functions))))))

(defn- reduce-step-dispatch [_ cmd]
  (cond (string? cmd)  :string
        (number? cmd)  :number
        (symbol? cmd)  :symbol
        (keyword? cmd) cmd
        (map? cmd)     FnCall
        :else          (fail "Unexpected opcode!" {:opcode cmd})))

(defmulti ^:private reduce-step reduce-step-dispatch)
(defmulti ^:private action-arity (partial reduce-step-dispatch []))

;; throws exception if there are operators out of place, returns input otherwise
(defn- validate-rpn [rpn]
  (let [steps (map #(- 1 (action-arity %)) rpn)]
    (if (or (not-every? pos? (reductions + steps)) (not (= 1 (reduce + steps))))
      (throw (ex-info (str "Wrong tokens, unsupported arities: " rpn) {:rpn rpn}))
      rpn)))

(defmethod call-fn :default [fn-name & args-seq]
  (if-let [default-fn (::functions *calc-vars*)]
    (default-fn fn-name args-seq)
    (throw (new IllegalArgumentException (str "Unknown function: " fn-name)))))

;; Gives access to whole input payload. Useful when top level keys contain strange characters.
;; Example: you can write data()['key1']['key2'] instead of key1.key2.
(defmethod call-fn "data" [_] *calc-vars*)

(defmethod action-arity FnCall [{:keys [args]}] args)

(defmethod reduce-step FnCall [stack {:keys [fn args]}]
  (try
    (log/trace "Calling function {} with arguments {}" fn args)
    (let [[ops new-stack] (split-at args stack)
          ops (reverse ops)
          result (apply call-fn fn ops)]
      (log/trace "Result was {}" result)
      (conj new-stack result))
    (catch clojure.lang.ArityException e
      (throw (ex-info (str "Wrong arity: " (.getMessage e))
                      {:fn fn :expected args :got (count ops) :ops (vec ops)})))))

(defmacro def-reduce-step [cmd args body]
  (assert (keyword? cmd))
  (assert (every? symbol? args))
  `(do (defmethod action-arity ~cmd [_#] ~(count args))
       (defmethod reduce-step ~cmd [[~@args ~'& stack#] action#]
         (let [~'+action+ action#] (conj stack# ~body)))))

(def-reduce-step :string [] +action+)
(def-reduce-step :number [] +action+)
(def-reduce-step :symbol [] (get-in *calc-vars* (vec (.split (name +action+) "\\."))))

(def-reduce-step :neg [s0] (- s0))
(def-reduce-step :times [s0 s1] (* s0 s1))
(def-reduce-step :divide [s0 s1] (with-precision 8 (/ s1 s0)))
(def-reduce-step :plus [s0 s1] (if (or (string? s0) (string? s1)) (str s1 s0) (+ s1 s0)))
(def-reduce-step :minus [s0 s1] (- s1 s0))
(def-reduce-step :eq [a b] (= a b))
(def-reduce-step :or [a b] (or b a))
(def-reduce-step :not [b] (not b))
(def-reduce-step :and [a b] (and b a))
(def-reduce-step :neq [a b] (not= a b))
(def-reduce-step :mod [s0 s1] (mod s1 s0))
(def-reduce-step :lt [s0 s1] (< s1 s0))
(def-reduce-step :lte [s0 s1] (<= s1 s0))
(def-reduce-step :gt [s0 s1] (> s1 s0))
(def-reduce-step :gte [s0 s1] (>= s1 s0))
(def-reduce-step :power [s0 s1] (Math/pow s1 s0))
(def-reduce-step :get [a b]
  (cond (sequential? b) (when (number? a) (get b (->int a)))
        (string? b)     (when (number? a) (get b (->int a)))
        (instance? java.util.List b) (when (number? a) (.get ^java.util.List b (->int a)))
        :else           (get b (str a))))

(defn eval-rpn
  ([bindings default-function tokens]
   (assert (ifn? default-function))
   (eval-rpn (assoc bindings ::functions default-function) tokens))
  ([bindings tokens]
   (assert (map? bindings))
   (assert (seq tokens))
   (binding [*calc-vars* bindings]
     (let [result (reduce reduce-step () tokens)]
       (assert (= 1 (count result)))
       (first result)))))

(def parse (comp validate-rpn tokens->rpn validate-tokens tokenize))

:OK
