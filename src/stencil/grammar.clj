(ns stencil.grammar)

(defn- third [x] (nth x 2))

(defn- expecting [pred]
  (fn [t]
    (when-some [v (pred (first t))] 
      [v (next t)])))

(defn- guarded [pred]
  (fn [t]
    (when (pred (first t))
      [(first t) (next t)])))

(assert (= [true [:a]] ((expecting keyword?) [:k :a])))

;; left-associative chained infix expression
(defn- chained [reader separator reducer]
  (fn [tokens]
    (when-let [[result tokens] (reader tokens)]
      (loop [tokens tokens
             result result]
        (if-let [[fs tokens] (separator tokens)]
          (if-let [[op2 tokens] (reader tokens)]
            (recur tokens (reducer result fs op2))
            (throw (ex-info "Unexpected charater!" {:tokens tokens})))
          [result tokens])))))

(assert (= [[[:a :+ :b] :+ :c] [:!!]]
           ((chained (expecting #{:a :b :c}) (expecting #{:+}) vector) [:a :+ :b :+ :c :!!])))

;; right sides should be all functions.

;; parses all readers or none
(defn- all [& readers]
;; TODO: if one has been read then it should thow error at that point
  (fn [tokens]
    (reduce (fn [[result tokens] reader]
              (if-let [[r tokens] ()]
                [(conj result r) tokens]
                (reduced nil)))
      [[] tokens] readers)))

#_
(defmacro grammar [bindings body]
  `(letfn* [~@(for [[k v] (partition 2 bindings), x [k (list 'fn '[%] (list v '%))]] x)] ~body))



(defn- parenthesed [reader]
  (all (expecting #{:open}) reader (expecting #{:close})))

#_
(def grammar
  (grammar [powers      (chained expression (expecting #{:pow}) vector)
            mults       (chained powers (expecting #{:mul :div}) vector)
            adds        (chained mults  (expecting #{:plus :minus}) vector)
            parenthesed (all (expecting :open) expression (expecting #{:close}))
            identifier  (guarded symbol?)
            accessed    (all identifier :open-bracked expression :close-bracked)
            fncall      (all (expecting #{:fncall}) (chained expression #{:comma} vector) (expectig #{:close}))
            string      (guarded string?)
            number      (guarded number?)
            value       (ether identifier string number)

            negated (all (optional (expecting #{:neg})) expression)
            not     (all (optional (expecting #{:not})) negated)
            or      (chained not (expecting #{:or}))
            and     (chained and (expecting #{:or}))
            expression  (either (parenthesed adds) adds accessed value)
          ]
    expression))