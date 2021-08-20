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
  (testing "With custom locale"
    (is (= "1 000 000,00" (call-fn "formatWithLocale" "hu" "%,.2f" (int 1000000)))))
  (testing "Integer formatting"
    (is (= "1,000,000.00" (call-fn "format" "%,.2f" (int 1000000))))
    (is (= "Hello null" (call-fn "format" "Hello %d" nil))))
  (testing "Character formatting"
    (is (= "Hello null" (call-fn "format" "Hello %c" nil)))
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


(deftest test-date
  (letfn [(date [& args] (.call io.github.erdos.stencil.functions.DateFunctions/DATE  (to-array args)))]
    (testing "two arguments"
      (is (= "2021/05/20" (date "YYYY/MM/d", "2021-05-20")))
      (is (= nil (date nil, "2021-05-20")))
      (is (= nil (date "YYYY/MM/d", nil))))
    (testing "three arguments"
      (is (= "2021 május 20" (date "hu" "YYYY MMMM d", "2021-05-20")))
      (is (= nil (date "hu" nil, "2021-05-20")))
      (is (= nil (date "hu" "YYYY MMMM d", nil)))
      (is (= nil (date nil "YYYY MMMM d", "2021-05-20"))))))

(deftest test-list
  (is (= [] (call-fn "list")))
  (is (= [1 nil 3] (call-fn "list" 1 nil 3))))

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

(import '[stencil.types ReplaceImage])
(def data-uri "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==")

(deftest test-replace-image
  (binding [stencil.model/*extra-files* (atom #{})]
    (is (instance? ReplaceImage (call-fn "replaceImage" data-uri))))
  (testing "Parsing errors"
    (is (thrown? ExceptionInfo (call-fn "replaceImage" nil)))
    (is (thrown? ExceptionInfo (call-fn "replaceImage" "not data uri")))
    (is (thrown? ExceptionInfo (call-fn "replaceImage" "data:image/unknown;base64,XXXXXXX")))
    (is (thrown? ExceptionInfo (call-fn "replaceImage" "data:image/png;lalala")))
    (is (thrown? ExceptionInfo (call-fn "replaceImage" "data:image/png;lalala,XXXXXXX")))))