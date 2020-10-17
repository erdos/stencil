(ns stencil.zap-test
  (:require [stencil.zap :refer [zap]]
            [clojure.zip :as zip]
            [clojure.test :refer [deftest testing is are]]))

#_
(deftest test-zap*

  (->
   (zap {:tag :a :content [{:tag :b} {:tag :c}]}
        DOWN
        RIGHT
        (CHECK (fn [x] (= (:tag x) :c))))
   (zip/node)
   (= {:tag :c})
   (is))

  (->
   (zap {:tag :a :content [{:tag :b} {:tag :c}]}
        DOWN
        (CHECK (fn [x] (= (:tag x) :c))))
   (nil?)
   (is))

  (->
   (zap {:tag :a :content [{:tag :b} {:tag :c}]}
        (HAS DOWN
             (CHECK (fn [x] (= (:tag x) :c)))))
   (some?)
   (is))

  )
