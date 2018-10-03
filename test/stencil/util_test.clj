(ns stencil.util-test
  (:require [clojure.test :refer [deftest testing is]]
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
