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

(defn- either
  ([a b] (fn [tokens] (or (a tokens) (b tokens))))
  ([a b c] (fn [tokens] (or (a tokens) (b tokens) (c tokens))))
  ([a b c d] (fn [t] (or (a t) (b t) (c t) (d t)))))

(defmacro grammar [bindings body]
  `(letfn* [~@(for [[k v] (partition 2 bindings), x [k (list 'fn '[%] (list v '%))]] x)] ~body))

(defn- parenthesed [reader]
  (all (expecting #{:open}) reader (expecting #{:close})))

(defn unchain [p op p2] (list op p p2))
; (defn unchainr [[op1 p1 p2] op2 p3] (list op1 p1 (list op2 p2 p3)))

(def testlang
  (grammar [num (either (guarded number?) (guarded symbol?) (guarded string?)) ;; add (parenthesed expression) here?
            neg (chained num (expecting #{:neg}) unchain)
            not (chained neg (expecting #{:not}) unchain)
            pow (chained not (expecting #{:pow}) unchain) ;; TODO: right-associative
            mul (chained pow (expecting '#{* /}) unchain)
            add (chained mul (expecting '#{+ -}) unchain)

            cmp  (chained add (expecting '#{< > <= >=}) unchain)
            cmpe (chained cmp (expecting '#{= <>}) unchain) ;; eq/neq

            and (chained cmpe (expecting '#{and}) unchain)
            or  (chained and (expecting '#{or}) unchain)
            expression or]
           expression))

(println (testlang '[123 + 344 + aaa * 34 + 1 = 34 or a < b]))

