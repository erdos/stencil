(ns stencil.merger
  "Token listaban a text tokenekbol kiszedi a parancsokat es action tokenekbe teszi."
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.data.xml :as xml]
            [stencil.postprocess.ignored-tag :as ignored-tag]
            [stencil
             [cleanup :refer :all]
             [types :refer :all]
             [tokenizer :as tokenizer]
             [util :refer [prefixes suffixes]]]))

(set! *warn-on-reflection* true)

(defn peek-next-text
  "Returns a lazy seq of text content characters from the token list."
  [tokens]
  ((fn f [stack tokens]
     (when-let [[t & ts] (seq tokens)]
       (if-let [text (:text t)]
         (concat (for [[t & trs] (suffixes text)]
                   {:char  t
                    :stack stack
                    :text-rest trs
                    :rest  ts})
                 (lazy-seq (f stack ts)))
         (recur (cons t stack) ts))))
   nil tokens))

(defn find-first-code [^String s]
  (assert (string? s))
  (let [ind        (.indexOf s (str open-tag))]
    (when-not (neg? ind)
      (let [str-before (.substring s 0 ind)
            after-idx  (.indexOf s (str close-tag))]
        (if (neg? after-idx)
          (cond-> {:action-part (.substring s (+ ind (count open-tag)))}
            (not (zero? ind)) (assoc :before (.substring s 0 ind)))
          (cond-> {:action (.substring s (+ ind (count open-tag))
                                       after-idx)}
            (not (zero? ind)) (assoc :before (.substring s 0 ind))
            (not (= (+ (count close-tag) after-idx) (count s)))
            (assoc :after (.substring s (+ (count close-tag) after-idx)))))))))

;; returns a map with keys:
;; :tokens -> a seq of tokens
;; :action-part -> if string ends with a part of an action marker then it contains.
(defn text-split-tokens [^String s]
  (assert (string? s))
  (loop [s      s
         output []]
    (if-let [x (some-> s find-first-code)]
      (if (:action-part x)
        {:tokens (if-let [b (:before x)] (conj output {:text b}) output)
         :action-part (:action-part x)}
        (recur (:after x)
               (if (seq (:before x))
                 (conj output {:text (:before x)} {:action (:action x)})
                 (conj output {:action (:action x)}))))
      (if (seq s)
        {:tokens (conj output {:text s})}
        {:tokens output}))))

(declare cleanup-runs)

(defn -find-open-tag [last-chars-count next-token-list]
  (assert (integer? last-chars-count))
  (assert (pos? last-chars-count))
  (assert (sequential? next-token-list))
  (when (= (drop last-chars-count open-tag)
           (take (- (count open-tag) last-chars-count)
                 (map :char (peek-next-text next-token-list))))
    (nth (peek-next-text next-token-list)
         (dec (- (count open-tag) last-chars-count)))))

(defn -last-chars-count [sts-tokens]
  (assert (sequential? sts-tokens))
  (when (:text (last sts-tokens))
    (some #(when (.endsWith
                  (str (apply str (:text (last sts-tokens)))) (apply str %))
             (count %))
          (prefixes open-tag))))

;; tries to parse action
(defn action-maybe-parsed [token]
  (if-let [action (:action token)]
    (let [parsed (tokenizer/text->cmd action)]
      (if true #_( ;; ha fragment.
           )
        {:action parsed}
        {:text (str open-tag " " action " " close-tag)}))
    token))

;; TODO: csak akkor parszoljuk fel, ha fragment
;; egyebkent text tokenne alakitjuk ugy, ahogyan volt.
;; lel.
(defn map-action-token [token] (action-maybe-parsed token))

(defn cleanup-runs-1 [token-list]
  (assert (sequential? token-list))
  (assert (:text (first token-list)))
  ;; feltehetjuk, hogy text token van elol.
  (let [sts (text-split-tokens (:text (first token-list)))]
    (if (:action-part sts)
      ;; Ha van olyan akcio resz, amit elkezdtunk de nem irtunk vegig...
      (let [next-token-list (cons {:text (:action-part sts)} (next token-list))
            [this that] (split-with #(not= (seq close-tag)
                                           (take (count close-tag) (map :char %)))
                                    (suffixes (peek-next-text next-token-list)))
            that        (if (empty? that)
                          (throw (ex-info "Tag is not closed? " {:read (first this)}))
                          (first (nth that (dec (count close-tag)))))]
        (concat
         (map map-action-token (:tokens sts))

         (let [ap (action-maybe-parsed {:action (apply str (map (comp :char first) this))})]
           (if (:action ap)
             (concat
              (list* ap (reverse (:stack that)))
              (if (seq (:text-rest that))
                (lazy-seq (cleanup-runs-1 (cons {:text (apply str (:text-rest that))} (:rest that))))
                (lazy-seq (cleanup-runs (:rest that)))))
             666 #_ (list* {:text (:action-part sts)}
                    (lazy-seq (cleanup-runs (rest token-list))))))))

      (if-let [last-chars-count (-last-chars-count (:tokens sts))]
        ;; an open tag starts at the end of the text node..
        (if-let [this (-find-open-tag last-chars-count (next token-list))]
          (let [action-txt (apply str (:text-rest this))
                ap         (when (not-empty action-txt)
                             (action-maybe-parsed {:action action-txt}))]
            (concat
             (map map-action-token (butlast (:tokens sts)))
             (when-let [s (seq (drop-last last-chars-count (:text (last (:tokens sts)))))]
               [{:text (apply str s)}])
             (when (:action ap) [ap])
             (reverse (:stack this))
             (let [[this2 that2] (split-with #(not= (seq close-tag)
                                                    (take (count close-tag) (map :char %)))
                                             (suffixes (peek-next-text (:rest this))))
                   that2        (if (empty? that2) (throw (ex-info "Tag is not closed?" {}))
                                    (first (nth that2 (dec (count close-tag)))))]
               (concat
                (when-let [action-txt (not-empty (apply str (map (comp :char first) this2)))]
                  [(action-maybe-parsed {:action action-txt})])
                (reverse (:stack that2))
                (when (seq (:text-rest that2))
                  [{:text (apply str (:text-rest that2))}])
                (lazy-seq (cleanup-runs (:rest that2))))))

             )
          (concat (map map-action-token (:tokens sts)) (cleanup-runs (next token-list))))
        (concat (map map-action-token (:tokens sts)) (cleanup-runs (next token-list)))))))

(defn cleanup-runs [token-list]
  (when-let [[t & ts] (seq token-list)]
    (if (:text t)
      (cleanup-runs-1 token-list)
      (cons t (lazy-seq (cleanup-runs ts))))))

(defn- map-token [token] (:action token token))

(defn parse-to-tokens-seq
  "Parses input and returns a token sequence."
  [input]
  (->> input
       (xml/parse)
       (ignored-tag/map-ignored-attr)
       (tokenizer/structure->seq)
       (cleanup-runs)
       (map map-token)))

:OK
