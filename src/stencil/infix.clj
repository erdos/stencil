(ns stencil.infix
  "Parsing and evaluating infix algebraic expressions.

  https://en.wikipedia.org/wiki/Shunting-yard_algorithm"
  (:require [stencil.util :refer :all]
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
   \( :open
   \) :close
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
  "A szam literalok igy kezdodnek"
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

(defn read-string-literal
  "Beolvas egy string literalt.
   Visszaad egy ketelemu vektort, ahol az elso elem a beolvasott string literal,
   a masodik elem a maradek karakter szekvencia."
  [characters]
  (letfn [(read-until [x]
            (loop [[c & cs] (next characters)
                   out      ""]
              (cond (nil? c) (throw (ex-info "String parse error"
                                             {:reason "Unexpected end of stream"}))
                    (= c (first "\\"))  (recur (next cs) (str out (first cs)))
                    (= c x)             [out cs]
                    :else               (recur cs (str out c)))))]
    (case (first characters)
      \" (read-until \") ;; programmer quotes
      \' (read-until \') ;; programmer quotes
      \“ (read-until \”) ;; english double quotes
      \‘ (read-until \’) ;; english single quotes
      \’ (read-until \’) ;; hungarian single quotes (felidezojel)
      \„ (read-until \”)))) ;; hungarian double quotes (macskakorom)


(defn read-number "Beolvas egy szamot.
   Visszad egy ketelemu vektort, ahol az elso elem a beolvasott szam,
   a masodik elem a maradek karakter szekvencia.
   A beolvasott szam vagy Double vagy Long."
  [characters]
  (let [content (take-while (set "1234567890._") characters)
        content ^String (apply str content)
        content (.replaceAll content "_" "")
        number  (if (some #{\.} content)
                  (Double/parseDouble content)
                  (Long/parseLong     content))]
    [number (drop (count content) characters)]))

;; TODO: harapni kell, ha ket szam token vagy egymas utan `1 1`
;; TODO harapni kell, ha ket operator token van egymas utan `op op`
;; TODO harapni kell, ha `,)` `)(` `()`  `(,` `,,` `,op` `op,`  `op)`  `(op` `1(` `)2`  szerepel!
(defn tokenize
  "Az eredeti stringbol token listat csinal."
  [original-string]
  (loop [[first-char & next-chars :as characters] (str original-string)
         tokens []]
    (cond
      (empty? characters)
      tokens

      (contains? #{\space \tab \newline} first-char)
      (recur next-chars tokens)

      (contains? #{\, \;} first-char)
      (recur next-chars (conj tokens :comma))

      (contains? ops2 [first-char (first next-chars)])
      (recur (next next-chars) (conj tokens (ops2 [first-char (first next-chars)])))

      (and (= \- first-char) (or (nil? (peek tokens)) (keyword? (peek tokens))))
      (recur next-chars (conj tokens :neg))

      (contains? ops first-char)
      (recur next-chars (conj tokens (ops first-char)))

      (contains? digits first-char)
      (let [[n tail] (read-number characters)]
        (recur tail (conj tokens n)))

      (#{\" \' \“ \‘ \’ \„} first-char)
      (let [[s tail] (read-string-literal characters)]
        (recur tail (conj tokens s)))

      :else
      (let [content (apply str (take-while identifier characters))]
        (if (seq content)
          (let [tail (drop-while #{\space \tab} (drop (count content) characters))]
            (if (= \( (first tail))
              (recur (next tail) (conj tokens (->FnCall content)))
              (recur tail (conj tokens (symbol content)))))
          (throw (ex-info (str "Unexpected character: " first-char)
                          {:character first-char})))))))

;; TODO: harapni kell, ha a vegrehajtas utan az opstack-ban van zarojel!
;; TODO: harapni kell ha illegalis allapot all elo!
(defn tokens->rpn
  "Classic Shunting-Yard Algorithm extension to handle vararg fn calls."
  [tokens]
  (loop [[e0 & next-expr :as expr]      tokens ;; bemeneti token lista
         opstack   ()     ;; shunting-yard algo verme
         result    []     ;; a kimeno rpn tokenek listaja

         ;; ha fuggvenyhivas van, ide mentjuk a fuggveny nevet
         functions ()]
    (cond
      (empty? expr) (into result (remove #{:open} opstack))

      (number? e0)
      (recur next-expr opstack (conj result e0) functions)

      (symbol? e0)
      (recur next-expr opstack (conj result e0) functions)

      (string? e0)
      (recur next-expr opstack (conj result e0) functions)

      (= :open e0)
      (recur next-expr (conj opstack :open) result (conj functions nil))

      (instance? FnCall e0)
      (recur next-expr (conj opstack :open) result
             (conj functions {:fn (:fn-name  e0)
                              :args (if (= :close (first next-expr)) 0 1)}))
      ;; (recur next-expr (conj opstack :fncall) result (conj functions {:fn e0}))

      (= :close e0)
      (let [[popped-ops [x & keep-ops]]
            (split-with #(and (not= :open %)) opstack)]
        (recur next-expr
               keep-ops
               (into result
                     (concat
                      (remove #{:comma} popped-ops)
                      (some-> functions first vector)))
               (next functions)))

      :otherwise
      (let [[popped-ops keep-ops]
            (split-with #(>= (precedence %) (precedence e0)) opstack)]
        (recur next-expr
               (conj keep-ops e0)
               (into result (remove #{:open :comma} popped-ops))
               (if (= :comma e0)
                 (if (first functions)
                   (update-peek functions update :args inc)
                   (throw (ex-info "Unexpected ',' character!" {})))
                 functions))))))

(defn reduce-step-dispatch [_ cmd]
  (cond (string? cmd)  :string
        (number? cmd)  :number
        (symbol? cmd)  :symbol
        (keyword? cmd) cmd
        (map? cmd)     FnCall
        :else (throw (ex-info (str "Unexpected opcode: " cmd) {:opcode cmd}))))

(defmulti ^:private reduce-step reduce-step-dispatch)
(defmulti ^:private action-arity (partial reduce-step-dispatch []))

(defn validate-rpn [rpn]
  (let [steps (map #(- 1 (action-arity %)) rpn)]
    (if (or (not-every? pos? (reductions + steps)) (not (= 1 (reduce + steps))))
      (throw (ex-info (str "Wrong tokens, unsupported arities" rpn " " (vec steps)) {:rpn rpn}))
      rpn)))

(defmethod call-fn :default [fn-name & args-seq]
  (if-let [default-fn (::functions *calc-vars*)]
    (default-fn fn-name args-seq)
    (throw (new IllegalArgumentException (str "Unknown function: " fn-name)))))

(defmethod action-arity FnCall [{:keys [args]}] args)

(defmethod reduce-step FnCall [stack {:keys [fn args]}]
  (try
    (let [[ops new-stack] (split-at args stack)
          ops (reverse ops)
          result (apply call-fn fn ops)]
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
(def-reduce-step :divide [s0 s1] (/ s1 s0))
(def-reduce-step :plus [s0 s1] (+ s0 s1))
(def-reduce-step :minus [s0 s1] (- s1 s0))
(def-reduce-step :eq [a b] (= a b))
(def-reduce-step :or [a b] (or a b))
(def-reduce-step :not [b] (not b))
(def-reduce-step :and [a b] (and a b))
(def-reduce-step :neq [a b] (not= a b))
(def-reduce-step :mod [s0 s1] (mod s0 s1))
(def-reduce-step :lt [s0 s1] (< s1 s0))
(def-reduce-step :lte [s0 s1] (<= s1 s0))
(def-reduce-step :gt [s0 s1] (> s1 s0))
(def-reduce-step :gte [s0 s1] (>= s1 s0))
(def-reduce-step :power [s0 s1] (Math/pow s0 s1))

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

(def parse (comp validate-rpn tokens->rpn tokenize))

:OK
