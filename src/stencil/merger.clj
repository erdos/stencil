(ns stencil.merger
  "Token listaban a text tokenekbol kiszedi a parancsokat es action tokenekbe teszi."
  (:require [clojure.data.xml :as xml]
            [stencil.postprocess.ignored-tag :as ignored-tag]
            [stencil
             [types :refer [open-tag close-tag]]
             [util :refer [parsing-exception]]
             [tokenizer :as tokenizer]]))

(set! *warn-on-reflection* true)

;; only fragment includes are evaluated
(def ^:dynamic *only-includes* false)

(defn map-action-token [{:keys [action]}]
  (let [parsed (tokenizer/text->cmd action)
        source (str open-tag action close-tag)
        parsed (assoc parsed :raw source)]
    (if (and *only-includes*
             (not= :cmd/include (:cmd parsed)))
      {:text source}
      {:action parsed})))

(defn- map-text-nodes [rf]
  (fn ([acc] (rf acc))
    ([acc x]
     (if (:text x)
       (reduce rf acc (:text x))
       (rf acc x)))))

(declare parse-upto-open-tag)

;; Constructs a function that reads the inside of a stencil expression until close-tag is reached.
;; The fn returns a collection when read fully or itself when there are characters left to read.
(defn- parse-until-close-tag [chars-and-tokens-to-append]
  (let [expected-close-tag-chars   (volatile! (seq close-tag))
        buffer-nonclose-chars-only (new java.util.ArrayList)
        buffer-all-read            (new java.util.ArrayList)]
    (fn self
      ([]
       (when (seq buffer-all-read)
         (throw (parsing-exception
                 "" (apply str "Stencil tag is not closed. Reading " open-tag buffer-nonclose-chars-only)))))
      ([token]
       (.add buffer-all-read token)
       (if (= token (first @expected-close-tag-chars))
         (when-not (vswap! expected-close-tag-chars next)
           (let [action (map-action-token {:action (apply str buffer-nonclose-chars-only)})]
             (if (:action action)
               (parse-upto-open-tag (concat [action]
                                        (remove char? chars-and-tokens-to-append)
                                        (remove char? buffer-all-read)))
               (parse-upto-open-tag (concat (vec chars-and-tokens-to-append)
                                        (vec buffer-all-read))))))
         (when (char? token)
           (vreset! expected-close-tag-chars (seq close-tag))
           (.clear buffer-nonclose-chars-only)
           (.addAll buffer-nonclose-chars-only (filter char? buffer-all-read))
           self))))))

;; Similar to the fn above. Consumes tokens up to the first open tag, then returns another parser (trampoline style).
(defn- parse-upto-open-tag [prepend]
  (let [expected-open-tag-chars (volatile! (seq open-tag))
        buffer                  (new java.util.ArrayList ^java.util.Collection prepend)]
    (fn self
      ([] buffer)
      ([token]
       (if (= token (first @expected-open-tag-chars))
         (if (= (count open-tag) (count @expected-open-tag-chars))
           (let [already-read (vec buffer)]
             (.clear buffer)
             (.add buffer token)
             (vswap! expected-open-tag-chars next)
             already-read)
           (do (.add buffer token)
               (when-not (vswap! expected-open-tag-chars next)
                 (parse-until-close-tag buffer))))
         (if (= (count open-tag) (count @expected-open-tag-chars))
           (let [result (concat (vec buffer) [token])]
             (.clear buffer) ;; reading an open-tag from start => we dump the content of buffer
             result)
           (if (char? token)
             (let [out (vec buffer)]
               (vreset! expected-open-tag-chars (seq open-tag))
               (.clear buffer)
               (if (= token (first @expected-open-tag-chars))
                 (do (.add buffer token)
                     (vswap! expected-open-tag-chars next)
                     out)
                 (concat out [token])))
             (do (.add buffer token)
                 self))))))))

(defn- parser-trampoline []
  (fn [rf]
    (let [parser (volatile! (parse-upto-open-tag []))]
      (fn
        ([acc] (rf (reduce rf acc (@parser))))
        ([acc token]
         (let [result (@parser token)]
           (if (fn? result)
             (do (vreset! parser result) acc)
             (reduce rf acc result))))))))

;; Transducer that merges consecutive characters into a text token, eg.: (1 \a \b \c 2) to (1 {:text "abc"} 2)
(defn- unmap-text-nodes []
  (let [tmp (volatile! true)]
    (comp (partition-by (fn [x] (when-not (char? x) (vswap! tmp not))))
          (map (fn [x] (if (every? char? x) {:text (apply str x)} (first x)))))))

(defn cleanup-runs [tokens-seq]
  (eduction (comp map-text-nodes (parser-trampoline) (unmap-text-nodes)) tokens-seq))

(defn- map-token [token] (:action token token))

(defn parse-to-tokens-seq
  "Parses input and returns a token sequence."
  [input]
  (->> input
       (xml/parse)
       (ignored-tag/map-ignored-attr)
       (tokenizer/structure->seq)
       (cleanup-runs)
       (eduction (map map-token))))

:OK
