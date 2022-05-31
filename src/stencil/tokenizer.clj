(ns stencil.tokenizer
  "Fog egy XML dokumentumot es tokenekre bontja"
  (:require [clojure.data.xml :as xml]
            [clojure.string :refer [starts-with?]]
            [stencil.infix :as infix]
            [stencil.types :refer [open-tag close-tag]]
            [stencil.util :refer [assoc-if-val mod-stack-top-conj mod-stack-top-last parsing-exception]]))

(set! *warn-on-reflection* true)

(defn- text->cmd-impl [^String text]
  (assert (string? text))
  (let [text (.trim text)
        pattern-elseif #"^(else\s*if|elif|elsif)(\(|\s+)"]
    (cond
      (#{"end" "endfor" "endif"} text) {:cmd :end}
      (= text "else") {:cmd :else}

      (starts-with? text "if ")
      {:cmd       :if
       :condition (infix/parse (subs text 3))}

      (starts-with? text "unless ")
      {:cmd       :if
       :condition (conj (vec (infix/parse (subs text 7))) :not)}

      (starts-with? text "for ")
      (let [[v expr] (vec (.split (subs text 4) " in " 2))]
        {:cmd        :for
         :variable   (symbol (.trim ^String v))
         :expression (infix/parse expr)})

      (starts-with? text "=")
      {:cmd        :echo
       :expression (infix/parse (subs text 1))}

      ;; fragment inclusion
      (starts-with? text "include ")
      {:cmd :cmd/include
       :name (first (infix/parse (subs text 8)))}

      ;; `else if` expression
      (seq (re-seq pattern-elseif text))
      (let [prefix-len (count (ffirst (re-seq pattern-elseif text)))]
        {:cmd :else-if
         :condition (infix/parse (subs text prefix-len))})

      :else (throw (ex-info "Unexpected command" {:command text})))))

(defn text->cmd [text]
  (try (text->cmd-impl text)
    (catch clojure.lang.ExceptionInfo e
      (throw (parsing-exception (str open-tag text close-tag) (.getMessage e))))))

(defn structure->seq [parsed]
  (cond
    (string? parsed)
    [{:text parsed}]

    (seq (:content parsed))
    (concat
     [(assoc-if-val {:open (:tag parsed)} :attrs (not-empty (:attrs parsed)))]
     (mapcat structure->seq (:content parsed))
     [{:close (:tag parsed)}])

    :else
    [(cond-> {:open+close (:tag parsed)}
       (seq (:attrs parsed)) (assoc :attrs (:attrs parsed)))]))

(defn- tokens-seq-reducer [stack token]
  (cond
    (:text token)
    (mod-stack-top-conj stack (:text token))

    (:open+close token)
    (let [elem (xml/element (:open+close token) (:attrs token))]
      (mod-stack-top-conj stack elem))

    (:open token)
    (let [elem (xml/element (:open token) (:attrs token))]
      (-> stack (mod-stack-top-conj elem) (conj [])))

    (:close token)
    (let [[s & stack] stack]
      (mod-stack-top-last stack assoc-if-val :content (not-empty s)))

    :else
    (throw (ex-info (str "Unexpected token: " token " of " (type token)) {:token token}))))

(defn tokens-seq->document
  "From token seq builds an XML tree."
  [tokens-seq]
  (let [start '(())
        result (reduce tokens-seq-reducer start tokens-seq)]
    (assert (= 1 (count result)) (str (pr-str result)))
    (assert (= 1 (count (first result))))
    (ffirst result)))

:OK
