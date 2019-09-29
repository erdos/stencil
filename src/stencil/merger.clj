(ns stencil.merger
  "Token listaban a text tokenekbol kiszedi a parancsokat es action tokenekbe teszi."
  (:require [clojure.test :refer [deftest testing is are]]
            [stencil
             [cleanup :refer :all]
             [types :refer :all]
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
      (let [after-idx  (.indexOf s (str close-tag))]
        (if (neg? after-idx)
          (cond-> {:action-part (.substring s (+ ind (count open-tag)))}
            (not (zero? ind)) (assoc :before (.substring s 0 ind)))
          (cond-> {:action (.substring s (+ ind (count open-tag))
                                       after-idx)}
            (not (zero? ind)) (assoc :before (.substring s 0 ind))
            (not (= (+ (count close-tag) after-idx) (count s)))
            (assoc :after (.substring s (+ (count close-tag) after-idx)))))))))

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

(defn -find-end-tag [last-chars-count next-token-list]
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

(defn cleanup-runs-1 [[first-token & rest-tokens]]
  (assert (:text first-token))
  (let [sts (text-split-tokens (:text first-token))]
    (if (:action-part sts)
      ;; Ha van olyan akcio resz, amit elkezdtunk de nem irtunk vegig...
      (let [next-token-list (cons {:text (:action-part sts)} rest-tokens)
            [this that] (split-with #(not= (seq close-tag)
                                           (take (count close-tag) (map :char %)))
                                    (suffixes (peek-next-text next-token-list)))
            that        (if (empty? that)
                          (throw (ex-info "Tag is not closed? " {:read (first this)}))
                          (first (nth that (dec (count close-tag)))))
            action-content (apply str (map (comp :char first) this))]
        (concat
         (:tokens sts)
         [{:action action-content}]
         (reverse (:stack that))
         (if (seq (:text-rest that))
           (lazy-seq (cleanup-runs-1 (cons {:text (apply str (:text-rest that))} (:rest that))))
           (lazy-seq (cleanup-runs (:rest that))))))
      (if-let [last-chars-count (-last-chars-count (:tokens sts))]
        (if-let [this (-find-end-tag last-chars-count rest-tokens)]
          (concat
           (butlast (:tokens sts))
           (when-let [s (seq (drop-last last-chars-count (:text (last (:tokens sts)))))]
             [{:text (apply str s)}])
           (lazy-seq
            (cleanup-runs-1
             (concat
              [{:text (str open-tag (apply str (:text-rest this)))}]
              (reverse (:stack this))
              (:rest this)))))
          (concat (:tokens sts) (cleanup-runs rest-tokens)))
        (concat (:tokens sts) (cleanup-runs rest-tokens))))))

(defn cleanup-runs [token-list]
  (when-let [[t & ts] (seq token-list)]
    (if (:text t)
      (cleanup-runs-1 token-list)
      (cons t (lazy-seq (cleanup-runs ts))))))

(def map-actions-in-token-list cleanup-runs)

:OK
