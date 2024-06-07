(ns stencil.grammar)

(defn- guarded [pred]
  (fn [t]
    (when (pred (first t))
      [(first t) (next t)])))

;; left-associative chained infix expression
(defn- chained [reader reader* reducer]
  (fn [tokens]
    (when-let [[result tokens] (reader tokens)]
      (loop [tokens tokens
             result result]
        (if (empty? tokens)
          [result nil]
          (if-let [[fs tokens] (reader* tokens)]
            (recur tokens (reducer result fs))
            [result tokens]))))))

(defn- read-or-throw [reader tokens]
  (or (reader tokens)
      (throw (ex-info (str "Invalid stencil expression!") {:reader reader :prefix tokens}))))

(defn- all [condition & readers]
  (fn [tokens]
    (when-let [[result tokens] (condition tokens)]
      (reduce (fn [[result tokens] reader]
                (let [[r tokens] (read-or-throw reader tokens)]
                  [(conj result r) tokens]))
              [[result] tokens] readers))))

(defmacro ^:private grammar [bindings body]
  `(letfn* [~@(for [[k v] (partition 2 bindings), x [k (list 'fn '[%] (list v '%))]] x)] ~body))

(defn- mapping [reader mapper]
  (fn [tokens]
    (when-let [[result tokens] (reader tokens)]
      [(mapper result) tokens])))

(defn- parenthesed [reader]
  (mapping (all (guarded #{:open}) reader (guarded #{:close})) second))

(defn- op-chain [operand operator]
  (chained operand (all operator operand) (fn [a [op b]] (list op a b))))

(defn- op-chain-r [operand operator]
  (mapping (chained (all operand) (all operator operand) (fn [a [op b]] (list* b op a)))
           (fn [a] (reduce (fn [a [op c]] [op c a]) (first a) (partition 2 (next a))))))

(defn- at-least-one [reader]
  (fn [tokens]
    (when-let [[result tokens] (reader tokens)]
      (loop [tokens tokens, result [result]]
        (if-let [[res tokens] (reader tokens)]
          (recur tokens (conj result res))
          [result tokens])))))

(defn- optional [reader] ;; always matches
  (fn [t] (or (reader t) [nil t])))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(def testlang
  (grammar [val  (some-fn iden-or-fncall
                          (parenthesed expression)
                          (guarded number?)
                          (guarded string?))
            iden (guarded symbol?)
            dotted      (mapping (all (guarded #{:dot}) iden) (comp name second))
            bracketed   (mapping (all (guarded #{:open-bracket}) expression (guarded #{:close-bracket})) second)
            args        (mapping (optional (chained (all expression) (all (guarded #{:comma}) expression) into))
                                 (fn [x] (take-nth 2 x)))
            args-suffix      (parenthesed args)
            iden-or-fncall   (mapping (all iden (optional args-suffix))
                                      (fn [[id xs]] (if xs (list* :fncall id xs) id)))
            accesses         (mapping (all val (optional (at-least-one (some-fn bracketed dotted))))
                                      (fn [[id chain]] (if chain (list* :get id chain) id)))
            neg  (some-fn (all (guarded #{:minus}) neg) accesses)
            not  (some-fn (all (guarded #{:not}) not) neg)
            pow  (op-chain-r not (guarded #{:power}))
            mul  (op-chain pow (guarded #{:times :divide :mod}))
            add  (op-chain mul (guarded #{:plus :minus}))
            cmp  (op-chain add (guarded #{:lt :gt :lte :gte}))
            cmpe (op-chain cmp (guarded #{:eq :neq}))
            and  (op-chain cmpe (guarded #{:and}))
            or   (op-chain and (guarded #{:or}))
            expression or]
           expression))

(defn runlang [grammar input]
  (ffirst (read-or-throw (all grammar {nil []}) input)))