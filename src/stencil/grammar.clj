(ns stencil.grammar)

(defn- expecting [pred]
  (fn [t]
    (when-some [v (pred (first t))] 
      [v (next t)])))

(assert (= [true [:a]] ((expecting keyword?) [:k :a])))

(defn- guarded [pred]
  (fn [t]
    (when (pred (first t))
      [(first t) (next t)])))

;; left-associative chained infix expression
(defn- chained [reader separator reducer]
  (fn [tokens]
    (when-let [[result tokens] (reader tokens)]
      (loop [tokens tokens
             result result]
        (if (empty? tokens)
          [result nil]
          (if-let [[fs tokens] (separator tokens)]
            (if-let [[op2 tokens] (reader tokens)]
              (recur tokens (reducer result fs op2))
              (throw (ex-info "Unexpected charater!" {:tokens tokens})))
            [result tokens]))))))

;(assert (= [[[:a :+ :b] :+ :c] [:!!]]
;           ((chained (expecting #{:a :b :c}) (expecting #{:+}) vector) [:a :+ :b :+ :c :!!])))

(defn- all [condition & readers]
  (fn [tokens]
    (when-let [[result tokens] (condition tokens)]
      (reduce (fn [[result tokens] reader]
                (if-let [[r tokens] (reader tokens)]
                  [(conj result r) tokens]
                  (throw (ex-info "Could not read!" {:reader reader :prefix tokens}))))
              [[result] tokens] readers))))

(defn- either
  ([a b] (fn [tokens] (or (a tokens) (b tokens))))
  ([a b c] (fn [tokens] (or (a tokens) (b tokens) (c tokens))))
  ([a b c d] (fn [t] (or (a t) (b t) (c t) (d t)))))

(defmacro grammar [bindings body]
  `(letfn* [~@(for [[k v] (partition 2 bindings), x [k (list 'fn '[%] (list v '%))]] x)] ~body))

(defn- mapping [reader mapper]
  (fn [tokens]
    (when-let [[result tokens] (reader tokens)]
      [(mapper result) tokens])))

(defn- parenthesed [reader]
  (mapping (all (expecting #{:open}) reader (expecting #{:close})) second))
; (assert (= [23 nil] ((parenthesed (guarded number?)) [:open 23 :close])))

(defn unchain [p op p2] (list op p p2))

(defn at-least-one [reader]
  (fn [tokens]
    (when-let [[result tokens] (reader tokens)]
      (loop [tokens tokens, result [result]]
        (if-let [[res tokens] (reader tokens)]
          (recur tokens (conj result res))
          [result tokens])))))

(defn optional [reader] ;; always matches
  (fn [tokens] (or (reader tokens) [nil tokens])))

(def testlang
  (grammar [val (either access-or-fncall
                        (parenthesed expression)
                        (guarded number?)
                        (guarded string?))
            iden (guarded symbol?)

            bracketed   (mapping (all (expecting #{:open-bracket}) expression (expecting #{:close-bracket})) second)
            args        (mapping (chained (all expression) (expecting #{:comma}) (fn [a _ c] (into a c))) (fn [x] [:b x]))
            access      (mapping (at-least-one bracketed) (fn [a] [:a a]))
            access-or-fncall (mapping (all iden (optional (either (parenthesed args) access)))
                                (fn [[sym args]]
                                  (case (first args)
                                    nil sym
                                    :a (list* :get sym (second args))
                                    :b (list* :fncall sym (second args))
                                  )))


            ;; TODO: fn call and commas

            neg (either (all (expecting '#{-}) val) val) ;; TODO; map
            not (either (all (expecting #{:not}) neg) neg) ;; TODO: map

            pow (chained not (expecting #{:pow}) unchain) ;; TODO: right-associative
            mul (chained pow (expecting '#{* / %}) unchain)
            add (chained mul (expecting '#{+ -}) unchain)
            cmp  (chained add (expecting '#{< > <= >=}) unchain)
            cmpe (chained cmp (expecting '#{= <>}) unchain) ;; eq/neq
            and (chained cmpe (expecting #{:and}) unchain)
            or  (chained and (expecting #{:or}) unchain)
            expression or]
           expression))

(defn runlang [grammar input]
  (if-let [[result tokens] (grammar input)]
    (if (empty? tokens)
      result
      (throw (ex-info "Expected EOF found characters" {})))
    (throw (ex-info "Could not parse" {}))))


(println (runlang testlang '[ 1 + 2 + 3]))

(println (runlang testlang '[ hello :open-bracket idx + 1 :close-bracket :open-bracket 4 :close-bracket * 2]))
(println (runlang testlang '[ :open 1 + 2 :close * 3]))
(println (runlang testlang '[123 + 344 + aaa * 34 + 1 = 34 :or a < b]))

(println (runlang testlang '[hello :open 1 :comma 2 :close]))
