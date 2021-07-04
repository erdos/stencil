(ns stencil.functions-test
  (:import [clojure.lang ExceptionInfo])
  (:require [stencil.functions :refer [call-fn]]
            [clojure.test :refer [deftest testing is are]]))


(deftest test-numerics
  (testing "decimal"
    (is (= (bigdec 0.23) (call-fn "decimal" 0.23)))
    (is (= nil (call-fn "decimal" nil))))
  (testing "integer"
    (is (= (biginteger 10) (call-fn "integer" 10)))
    (is (= nil (call-fn "integer" nil)))
    (is (= (biginteger 10) (call-fn "integer" (bigdec 10.2))))
    (is (= (biginteger 10) (call-fn "integer" (double 10))))))

(deftest test-format
  (is (= "hello 42" (call-fn "format" "hello %d" 42)))
  (is (= "hello 42" (call-fn "format" "hello %d" 42.0)))
  (testing "Character formatting"
    (is (= "Hello x" (call-fn "format" "Hello null" nil)))
    (is (= "Hello x" (call-fn "format" "Hello %c" "x")))
    (is (= "Hello X" (call-fn "format" "Hello %C" \x))))
  (testing "Indexed parameters"
    (is (= "hello 42 41.00" (call-fn "format" "hello %2$d %1$,.2f" 41.0 42.0))))
  (is (= "hello john" (call-fn "format" "hello %s" "john")))
  (testing "Error handling"
    (is (thrown? clojure.lang.ArityException (call-fn "format")))
    (is (thrown? ExceptionInfo (call-fn "format" "pattern")))
    (is (thrown? ExceptionInfo (call-fn "format" 34 1)))
    (is (thrown? ExceptionInfo (call-fn "format" nil 2)))))

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
