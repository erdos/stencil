(ns stencil.util-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.zip :as zip]
            [stencil.util :refer :all]))

(deftest stacks-difference-test
  (testing "Empty cases"
    (is (= [[] []] (stacks-difference nil nil)))
    (is (= [[] []] (stacks-difference () ())))
    (is (= [[] []] (stacks-difference '(:a :b :c) '(:a :b :c)))))

  (testing "simple cases"
    (is (= [[:a :b] []] (stacks-difference '(:a :b) ())))
    (is (= [[] [:a :b]] (stacks-difference '() '(:a :b))))
    (is (= [[:a] [:b]] (stacks-difference '(:a :x :y) '(:b :x :y))))
    (is (= [[:a] []] (stacks-difference '(:a :x :y) '(:x :y))))
    (is (= [[] [:b]] (stacks-difference '(:x :y) '(:b :x :y))))))

(deftest mod-stack-top-last-test
  (testing "Invalid input"
    (is (thrown? IllegalStateException (mod-stack-top-last '([]) inc)))
    (is (thrown? NullPointerException (mod-stack-top-last '() inc))))

  (testing "simple cases"
    (is (= '([3]) (mod-stack-top-last '([2]) inc)))
    (is (= '([1 1 2] [1 1 1])
           (mod-stack-top-last '([1 1 1] [1 1 1]) inc)))))

(deftest mod-stack-top-conj-test
  (testing "empty input"
    (is (= '([2]) (mod-stack-top-conj '() 2)))
    (is (= '([2]) (mod-stack-top-conj '([]) 2))))

  (testing "simple cases"
    (is (= '([1 2]) (mod-stack-top-conj '([1]) 2)))
    (is (= '([1 1 1] [2 2] [3 3])
           (mod-stack-top-conj '([1 1] [2 2] [3 3]) 1)))))

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

(deftest concatv-test
  (is (vector? (concatv)))
  (is (= [] (concatv)))
  (is (= [] (concatv nil)))
  (is (= [] (concatv nil nil)))
  (is (= [1] (concatv '(1))))
  (is (= [1 2 3 4] (concatv '(1 2) '(3 4)))))
