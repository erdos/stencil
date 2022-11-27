(ns stencil.util-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.zip :as zip]
            [stencil.util :refer :all]))

(deftest stacks-difference-test
  (testing "Empty cases"
    (is (= [[] []] (stacks-difference-key identity nil nil)))
    (is (= [[] []] (stacks-difference-key identity () ())))
    (is (= [[] []] (stacks-difference-key identity '(:a :b :c) '(:a :b :c)))))

  (testing "simple cases"
    (is (= [[:a :b] []] (stacks-difference-key identity '(:a :b) ())))
    (is (= [[] [:a :b]] (stacks-difference-key identity '() '(:a :b))))
    (is (= [[:a] [:b]] (stacks-difference-key identity '(:a :x :y) '(:b :x :y))))
    (is (= [[:a] []] (stacks-difference-key identity '(:a :x :y) '(:x :y))))
    (is (= [[] [:b]] (stacks-difference-key identity '(:x :y) '(:b :x :y))))))

(deftest mod-stack-top-last-test
  (testing "Invalid input"
    (is (thrown? IllegalStateException (mod-stack-top-last '([]) inc)))
    (is (thrown? IllegalStateException (mod-stack-top-last '() inc))))

  (testing "simple cases"
    (is (= '([3]) (mod-stack-top-last '([2]) inc)))
    (is (= '([1 1 2] [1 1 1])
           (mod-stack-top-last '([1 1 1] [1 1 1]) inc)))))

(deftest mod-stack-top-conj-test
  (testing "empty input"
    (is (thrown? IllegalStateException (mod-stack-top-conj '() 2)))
    (is (= '([2]) (mod-stack-top-conj '([]) 2))))

  (testing "simple cases"
    (is (= '([1 2]) (mod-stack-top-conj '([1]) 2)))
    (is (= '([1 1 1] [2 2] [3 3])
           (mod-stack-top-conj '([1 1] [2 2] [3 3]) 1)))
    (is (= '([1 1 1 2 3] [2 2] [3 3])
           (mod-stack-top-conj '([1 1] [2 2] [3 3]) 1 2 3)))))

(deftest update-peek-test
  (testing "simple cases"
    (is (thrown? IllegalStateException (update-peek [] inc)))
    (is (= [1 1 1 2] (update-peek [1 1 1 1] inc)))))

(deftest xml-zip-test
  (testing "XML nodes are always branches"
    (testing "Clojure Core xml-zip"
      (is (zip/branch? (zip/xml-zip {:tag "A"})))
      (is (not (zip/branch? (zip/xml-zip "child"))))
      (is (zip/branch? (zip/xml-zip 42))))
    (testing "Stencil's xml-zip"
      (is (zip/branch? (xml-zip {:tag "A"})))
      (is (not (zip/branch? (xml-zip "child"))))
      (testing "Difference clojure core"
        (is (not (zip/branch? (xml-zip 42))))))))

(deftest test-suffixes
  (is (= [] (suffixes nil)))
  (is (= [] (suffixes [])))
  (is (= [[1]] (suffixes [1])))
  (is (= [[1 2 3] [2 3] [3]] (suffixes [1 2 3]))))

(deftest test-prefixes
  (is (= [] (prefixes nil)))
  (is (= [] (prefixes [])))
  (is (= [[1]] (prefixes [1])))
  (is (= [[1 2 3] [1 2] [1]] (prefixes [1 2 3]))))

(deftest test-->int
  (is (= nil (->int nil)))
  (is (= 23 (->int 23)))
  (is (= 23 (->int "23")))
  (is (= 23 (->int 23.2)))
  (is (thrown? clojure.lang.ExceptionInfo (->int :asdf))))

(deftest update-some-test
  (is (= nil (update-some nil [:a] inc)))
  (is (= {:a 1} (update-some {:a 1} [:b] inc)))
  (is (= {:a 2 :x 1} (update-some {:a 1 :x 1} [:a] inc)))
  (is (= {:a 1 :x 1} (update-some {:a 1 :x 1} [:a] #{}))))

(deftest fixpt-test
  (is (= nil (fixpt first [])))
  (is (= :a (fixpt {:a :a :b :a :c :b} :c))))

(deftest find-first-test
  (is (= 1 (find-first odd? [0 1 2 3 4])))
  (is (= nil (find-first odd? [0 2 4])))
  (is (= nil (find-first odd? []) (find-first odd? nil))))

(deftest find-last-test
  (is (= 3 (find-last odd? [0 1 2 3 4])))
  (is (= nil (find-last odd? [0 2 4])))
  (is (= nil (find-last odd? []) (find-last odd? nil))))

(deftest fail-test
  (is (thrown? clojure.lang.ExceptionInfo (fail "test error" {}))))

(deftest prefixes-test
  (is (= [] (prefixes []) (prefixes nil)))
  (is (= [[1 2 3] [1 2] [1]] (prefixes [1 2 3]))))

(deftest suffixes-test
  (is (= [] (suffixes []) (suffixes nil)))
  (is (= [[1 2 3] [2 3] [3]] (suffixes [1 2 3]))))

(deftest whitespace?-test
  (is (= true (whitespace? \space)))
  (is (= true (whitespace? \tab)))
  (is (= false (whitespace? " ")))
  (is (= false (whitespace? \A))))
