(ns stencil.postprocess.fragments-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.zip :as zip]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer [find-first-in-tree xml-zip]]
            [stencil.postprocess.fragments :refer :all]))

;; make all private vars public!
(let [target (the-ns 'stencil.postprocess.fragments)]
  (doseq [[k v] (ns-map target)
          :when (and (var? v) (= target (.ns ^clojure.lang.Var v)))]
    (eval `(defn ~(symbol (str "-" k)) [~'& args#] (apply (deref ~v) args#)))))


(defn- p [& xs] {:tag ooxml/p :content xs})
(defn- r [& xs] {:tag ooxml/r :content xs})
(defn- t [& xs] {:tag ooxml/t :content xs})

;; TODO: before-after are gone!
(deftest test-split-paragraphs-1
  (let [data {:tag :root :content [(p (r (t "hello" :HERE) (t "vilag")))]}
        loc (find-first-in-tree #{:HERE} (xml-zip data))]
    (-> loc
        (doto assert)
        (-split-paragraphs (p (r (t "one"))) (p (r (t "two"))))
        (zip/root)
        ; (zip/node)
        (->> (println :!!!)))))
