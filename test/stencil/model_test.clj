(ns stencil.model-test
  (:require [stencil.model :refer :all]
            [clojure.test :refer [deftest is are testing]]))


(let [target (the-ns 'stencil.model)]
  (doseq [[k v] (ns-map target)
          :when (and (var? v) (= target (.ns v)))]
    (eval `(defn ~(symbol (str "-" k)) [~'& args#] (apply (deref ~v) args#)))))


(deftest test-add-styles
  (testing "A elems should be merged, second B elem should be renamed."
    (->
     (-add-styles
      {:tag "styles"
       :content [{:tag tag-style
                  :attrs {attr-style-id "A"}}
                 {:tag tag-style
                  :attrs {attr-style-id "B"}}]}
      {:tag "styles"
       :content [{:tag tag-style
                  :attrs {attr-style-id "A"}}
                 {:tag tag-style
                  :attrs {attr-style-id "B"
                          :x 1}}]})
     (:xml) (:content) (count) (= 3) (is))))
