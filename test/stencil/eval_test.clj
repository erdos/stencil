(ns stencil.eval-test
  (:require [clojure.test :refer [testing is are deftest]]
            [stencil.eval :refer :all]))

(def -text1- {:text "text1"})

(def ^:private test-data
  {"truthy" true
   "falsey" false
   "abc" {"def" "Okay"}
   "list0" []
   "list1" [1]
   "list3" [1 2 3]})

(defn- test-eval [input expected]
  (is (= expected
         (normal-control-ast->evaled-seq test-data {} input))))

(deftest test-no-change
  (test-eval [{:open "a"} {:close "a"}]
             [{:open "a"} {:close "a"}]))

(deftest test-if
  (testing "THEN branch"
    (test-eval [-text1-
                {:cmd :if :condition '[truthy]
                 :then [{:text "ok"}]
                 :else [{:text "err"}]}
                -text1-]
               [-text1-
                {:text "ok"}
                -text1-]))

  (testing "ELSE branch"
    (test-eval [-text1-
                {:cmd :if :condition '[falsey]
                 :then [{:text "ok"}]
                 :else [{:text "err"}]}]
               [-text1-
                {:text "err"}])))

(deftest test-echo
  (testing "Simple math expression"
    (test-eval [{:cmd :echo :expression '[1 2 :plus]}]
               [{:text "3"}]))
  (testing "Nested data access with path"
    (test-eval [{:cmd :echo :expression '[abc.def]}]
               [{:text "Okay"}])))

(deftest test-for
  (testing "loop without any items"
    (test-eval [{:cmd :for
                 :variable "index"
                 :expression '[list0]
                 :body-run-once [{:text "xx"}]
                 :body-run-none [{:text "meh"}]
                 :body-run-next [{:text "x"}]}]
               [{:text "meh"}]))

  (testing "loop with exactly 1 item"
    (test-eval [{:cmd :for
                 :variable "index"
                 :expression '[list1]
                 :body-run-once [{:cmd :echo :expression '[index]}]
                 :body-run-none [{:text "meh"}]
                 :body-run-next [{:text "x"}]}]
               [{:text "1"}]))

  (testing "loop with exactly 3 items"
    (test-eval [{:cmd :for
                 :variable "index"
                 :expression '[list3]
                 :body-run-once [{:cmd :echo :expression '[index]}]
                 :body-run-none [{:text "meh"}]
                 :body-run-next [{:text "x"} {:cmd :echo :expression '[index]}]}]
               [{:text "1"} {:text "x"} {:text "2"} {:text "x"} {:text "3"}])))
