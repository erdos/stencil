(ns stencil.grammar)

(defn expecting [pred]
  (fn [t]
    (when-let [v (pred (first t))] 
      [v (next t)])))

#_
(defn- chained [reader separator-reader]
  (fn [tokens]
    ()
  ))

#_
(let [powers (chained xx (expecting #{:pow}))
      mults  (chained powers (expecting #{:mul :div}))]
      
      )


(defn chained [reader separator]
  (fn f [t]
       (when-let [fst# (reader (first t#))]
         (if-let [sep# (separator (second t#))]
           (when-let [thr# (reader (third t#))]
             (recur whatever)))
         )))

;; right sides should be all functions.

(defmacro grammar [bindings body]
  `(letfn* [~@(for [[k v] (partition 2 bindings), x [k (list 'fn '[%] (list v '%))]] x)] ~body))

(def grammar
  (grammar [powers      (chained expression (expecting #{:pow}))
            mults       (chained powers (expecting #{:mul :div}))
            adds        (chained mults  (expecting #{:plus :minus}))
            parenthesed (all (expecting :open) expression (expecting #{:close}))
            identifier  (expecting symbol?)
            accessed    (all identifier :open-bracked expression :close-bracked)
            fncall      (all (expecting :fncall) (chained expression #{:comma}) (:expectig :close))
            string      (expecting string?)
            number      (expecting number?)
            value       (ether identifier string number)

            negated (all (optional (expecting #{:neg})) expression)
            not     (all (optional (expecting #{:not})) negated)
            power   (chained not (expecting #{:pow}))
            expression  (either (parenthesed adds) adds accessed value)
          ]
    expression))