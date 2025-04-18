(ns stencil.tokenizer
  "Fog egy XML dokumentumot es tokenekre bontja"
  (:require [clojure.data.xml :as xml]
            [clojure.string :refer [includes? split]]
            [stencil.infix :as infix]
            [stencil.util :refer [assoc-some mod-stack-top-conj mod-stack-top-last parsing-exception trim]]))

(set! *warn-on-reflection* true)

(defn- text->cmd-impl [^String text]
  (assert (string? text) (str "Not string: " (pr-str text)))
  (let [text (trim text)
        pattern-elseif #"^(else\s*if|elif|elsif)(\(|\s+)"]
    (cond
      (#{"end" "endfor" "endif"} text) {:cmd :cmd/end}
      (= text "else") {:cmd :cmd/else}

      (.startsWith text "if ")
      {:cmd       :cmd/if
       :condition (infix/parse (.substring text 3))}

      (.startsWith text "unless ")
      {:cmd       :cmd/if
       :condition (list :not (infix/parse (.substring text 7)))}

      (.startsWith text "for ")
      (let [[v expr] (split (subs text 4) #" in " 2)
             [idx v] (if (includes? v ",") (split v #",") ["$" v])]
        {:cmd        :cmd/for
         :variable   (symbol (trim v))
         :index-var  (symbol (trim idx))
         :expression (infix/parse expr)})

      (.startsWith text "=")
      {:cmd        :cmd/echo
       :expression (infix/parse (.substring text 1))}

      ;; fragment inclusion
      (.startsWith text "include ")
      {:cmd :cmd/include
       :name (infix/parse (.substring text 8))}

      ;; `else if` expression
      (seq (re-seq pattern-elseif text))
      (let [prefix-len (count (ffirst (re-seq pattern-elseif text)))]
        {:cmd :cmd/else-if
         :condition (infix/parse (.substring text prefix-len))})

      :else (throw (ex-info (str "Unexpected command: " text) {})))))

(defn text->cmd [text]
  (try (text->cmd-impl text)
       (catch clojure.lang.ExceptionInfo e
         (throw (parsing-exception text (.getMessage e))))))

(defn structure->seq [parsed]
  (cond
    (string? parsed)
    [{:text parsed}]

    (seq (:content parsed))
    (concat
     [(assoc-some {:open (:tag parsed)} :attrs (not-empty (:attrs parsed)))]
     (mapcat structure->seq (:content parsed))
     [{:close (:tag parsed)}])

    :else
    [(assoc-some {:open+close (:tag parsed)} :attrs (not-empty (:attrs parsed)))]))

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
      (mod-stack-top-last stack assoc-some :content (not-empty s)))

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
