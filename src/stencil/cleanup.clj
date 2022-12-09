(ns stencil.cleanup
  "

  This namespace annotates and normalizes Control AST.

  data flow is the following:

  valid XML String -> tokens -> Annotated Control AST -> Normalized Control AST -> Evaled AST -> Hiccup or valid XML String
  "
  (:require [stencil.util :refer [mod-stack-top-conj mod-stack-top-last parsing-exception stacks-difference-key]]
            [stencil.types :refer [open-tag close-tag]]))

(set! *warn-on-reflection* true)

(defn- tokens->ast-step [[queue & ss0 :as stack] token]
  (case (:cmd token)
    (:if :for) (conj (mod-stack-top-conj stack token) [])

    :else
    (if (empty? ss0)
      (throw (parsing-exception (str open-tag "else" close-tag)
                                "Unexpected {%else%} tag, it must come right after a condition!"))
      (conj (mod-stack-top-last ss0 update ::blocks (fnil conj []) {::children queue}) []))

    :else-if
    (if (empty? ss0)
      (throw (parsing-exception (str open-tag "else if" close-tag)
                                "Unexpected {%else if%} tag, it must come right after a condition!"))
      (-> ss0
          (mod-stack-top-last update ::blocks (fnil conj []) {::children queue})
          (conj [(assoc token :cmd :if :r true)])
          (conj [])))

    :end
    (if (empty? ss0)
      (throw (parsing-exception (str open-tag "end" close-tag)
                                "Too many {%end%} tags!"))
      (loop [[queue & ss0] stack]
        (let [new-stack (mod-stack-top-last ss0 update ::blocks conj {::children queue})]
          (if (:r (peek (first new-stack)))
            (recur (mod-stack-top-last new-stack dissoc :r))
            new-stack))))

    (:echo nil :cmd/include)
    (mod-stack-top-conj stack token)))

(defn tokens->ast
  "Flat token listabol nested AST-t csinal (listak listai)"
  [tokens]
  (let [result (reduce tokens->ast-step '([]) tokens)]
    (if (not= 1 (count result))
      (throw (parsing-exception (str open-tag "end" close-tag)
                                "Missing {%end%} tag from document!"))
      (first result))))

(defn- nested-tokens-fmap-postwalk
  "Depth-first traversal of the tree."
  [f-cmd-block-before f-cmd-block-after f-child node]
  (assert (map? node))  
  (letfn [(children-mapper [children]
            (mapv update-blocks children))
          (update-children [node]
            (update node ::children children-mapper))
          (visit-block [block]
            (-> block f-cmd-block-before update-children f-cmd-block-after))
          (blocks-mapper [blocks]
            (mapv visit-block blocks))
          (update-blocks [node]
            (if (:cmd node)
              (update node ::blocks blocks-mapper)
              (f-child node)))]
    (update-blocks node)))

(defn annotate-environments
  "Puts the context of each element into its :before and :after keys."
  [control-ast]
  (assert (sequential? control-ast))
  (let [stack (volatile! ())]
    (mapv (partial nested-tokens-fmap-postwalk
            (fn before-cmd-block [block]
              (assoc block ::before @stack))

            (fn after-cmd-block [block]
              (let [stack-before (::before block)
                    [a b]        (stacks-difference-key :open stack-before @stack)]
                (assoc block ::before a ::after b)))

            (fn child [item]
              (cond
                (:open item)
                (vswap! stack conj item)

                (:close item)
                (if (= (:close item) (:open (first @stack)))
                  (vswap! stack next)
                  (throw (ex-info "Unexpected stack state" {:stack @stack, :item item}))))
              item))
          control-ast)))

(defn stack-revert-close
  "Creates a seq of :close tags for each :open tag in the list in reverse order."
  [stack]
  (into () (comp (keep :open) (map #(do {:close %}))) stack))

;; egy {:cmd ...} parancs objektumot kibont:
;; a :blocks kulcs alatt levo elemeket normalizalja es specialis kulcsok alatt elhelyezi
;; igy amikor vegrehajtjuk a parancs objektumot, akkor az eredmeny is
;; valid fa lesz, tehat a nyito-bezaro tagek helyesen fognak elhelyezkedni.
(defmulti control-ast-normalize :cmd)

;; Itt nincsen blokk, amit normalizálni kellene
(defmethod control-ast-normalize :echo [echo-command] echo-command)

(defmethod control-ast-normalize :cmd/include [include-command]
  (if-not (string? (:name include-command))
    (throw (parsing-exception (pr-str (:name include-command))
                              "Parameter of include call must be a single string literal!"))
    include-command))

;; A feltételes elágazásoknál mindig generálunk egy javított THEN ágat
(defmethod control-ast-normalize :if [control-ast]
  (case (count (::blocks control-ast))
    2 (let [[then else] (::blocks control-ast)
            then2 (concat (map control-ast-normalize (::children then))
                          (stack-revert-close (::before else))
                          (::after else))
            else2 (concat (stack-revert-close (::before then))
                          (::after then)
                          (map control-ast-normalize (::children else)))]
        (-> (dissoc control-ast ::blocks)
            (assoc :then (vec then2) :else (vec else2))))

    1 (let [[then] (::blocks control-ast)
            else   (::after then)]
        (-> (dissoc control-ast ::blocks)
            (assoc :then (mapv control-ast-normalize (::children then)) :else (vec else))))
    ;; default
    (throw (parsing-exception (str open-tag "else" close-tag)
                              "Too many {%else%} tags in one condition!"))))

;; Egy ciklusnal kulon kell valasztani a kovetkezo eseteket:
;; - body-run-none: a body resz egyszer sem fut le, mert a lista nulla elemu.
;; - body-run-once: a body resz eloszor fut le, ha a lista legalabb egy elemu
;; - body-run-next: a body resz masodik, harmadik, stb. beillesztese, haa lista legalabb 2 elemu.
;; Ezekbol az esetekbol kell futtataskor a megfelelo(ket) kivalasztani es behelyettesiteni.
(defmethod control-ast-normalize :for [control-ast]
  (when-not (= 1 (count (::blocks control-ast)))
    (throw (parsing-exception (str open-tag "else" close-tag)
                              "Unexpected {%else%} in a loop!")))
  (let [[{::keys [children before after]}] (::blocks control-ast)
        children (mapv control-ast-normalize children)]
    (-> control-ast
        (dissoc ::blocks)
        (assoc :body-run-none (vec (concat (stack-revert-close before) after))
               :body-run-once (vec children)
               :body-run-next (vec (concat (stack-revert-close after) before children))))))

(defmethod control-ast-normalize :default [control-ast]
  (assert (not (::blocks control-ast)))
  control-ast)

(defn find-variables [control-ast]
  ;; meg a normalizalas lepes elott
  ;; amikor van benne blocks
  ;; mapping: {Sym -> Str}
  (letfn [(resolve-sym [mapping s]
            (assert (map? mapping))
            (assert (symbol? s))
            ;; megprobal egy adott szimbolumot a mapping alapjan rezolvalni.
            ;; visszaad egy stringet
            (if (.contains (name s) ".")
              (let [[p1 p2] (vec (.split (name s) "\\." 2))]
                (if-let [pt (mapping (symbol p1))]
                  (str pt "." p2)
                  (name s)))
              (mapping s (name s))))
          (expr [mapping e]
                ; (assert (sequential? rpn)) ;; RPN kifejezes kell legyen
                (cond (symbol? e)           [(resolve-sym mapping e)]
                      (not (sequential? e)) nil
                      (= :fncall (first e)) (mapcat (partial expr mapping) (nnext e))
                      :else                 (mapcat (partial expr mapping) (next e))))
          (maybe-variable [mapping e]
            (when (symbol? e) (resolve-sym mapping e)))
          (collect [m xs] (mapcat (partial collect-1 m) xs))
          (collect-1 [mapping x]
                     (case (:cmd x)
                       :echo (expr mapping (:expression x))

                       :if   (concat (expr mapping (:condition x))
                                     (collect mapping (apply concat (::blocks x))))

                       :for  (let [variable (maybe-variable mapping (:expression x))
                                   exprs    (expr mapping (:expression x))
                                   mapping  (if variable
                                              (assoc mapping (:variable x) (str variable "[]"))
                                              mapping)]
                               (concat exprs (collect mapping (apply concat (::blocks x)))))
                       []))]
    (distinct (collect {} control-ast))))

(defn- find-fragments [control-ast]
  ;; returns a set of fragment names use in this document
  (set (for [item (tree-seq map? (comp flatten ::blocks) {::blocks [control-ast]})
             :when (map? item)
             :when (= :cmd/include (:cmd item))]
         (:name item))))

(defn process [raw-token-seq]
  (let [ast (tokens->ast raw-token-seq)
        executable (mapv control-ast-normalize (annotate-environments ast))]
    {:variables  (find-variables ast)
     :fragments  (find-fragments ast)
     :dynamic?   (boolean (some :cmd executable))
     :executable executable}))

:OK
