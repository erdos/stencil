(ns stencil.functions-test
  (:import [clojure.lang ExceptionInfo])
  (:require [stencil.functions :refer [call-fn]]
            [clojure.test :refer [deftest testing is are]]))


(deftest test-map
  (testing "Empty input"
    (is (= [] (call-fn "map" "x" [])))
    (is (= [] (call-fn "map" "x" nil)))
    (is (= [] (call-fn "map" "x" {}))))
  (testing "Simple cases"
    (is (= [1 2 3]
           (call-fn "map" "x" [{:x 1} {:x 2} {:x 3}]))))
  (testing "Nested input"
    (is (= [1 2 3]
           (call-fn "map" "x.y"
                    [{:x {:y 1}} {:x {:y 2}} {:x {:y 3}}]))))
  (testing "Invalid input"
    (is (thrown? ExceptionInfo (call-fn "map" "x" "not-a-sequence")))
    (is (thrown? ExceptionInfo (call-fn "map" "x" {:x 1 :y 2})))
    (is (thrown? ExceptionInfo (call-fn "map" 1 [])))))
