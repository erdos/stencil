(ns stencil.merger-test
  (:require [stencil.merger :refer :all]
            [stencil.types]
            [clojure.test :refer [deftest testing is are use-fixtures]]))

(def map-action-token' map-action-token)

(use-fixtures :each (fn [f] (with-redefs [map-action-token identity] (f))))

(deftest cleanup-runs-test
  (testing "Simple cases"
    (are [x expected] (= expected (cleanup-runs x))
      [{:text "{%1234%}"} {:text "{%a%}{%b%}"}]
      [{:action "1234"} {:action "a"} {:action "b"}]

      [{:text "{"} {:text "%a%}"}]
      [{:action "a"}]

      [{:text "{%"} {:text "b%}"}]
      [{:action "b"}]

      [{:text "a{%1234%}b{%5678%}c"}]
      [{:text "a"} {:action "1234"} {:text "b"} {:action "5678"} {:text "c"}]))

  (testing "Simple embedding"
    (are [x expected] (= expected (cleanup-runs x))
      [{:open "A"} {:text "eleje {% a %} kozepe {% b %} vege"}]
      '({:open "A"} {:text "eleje "} {:action " a "} {:text " kozepe "} {:action " b "} {:text " vege"})))

  (testing "Base cases"
    (are [x expected] (= expected (cleanup-runs x))
      [{:text "asdf{%123"} {:open "a"} {:text "456%}xyz"}]
      [{:text "asdf"} {:action "123456"} {:open "a"} {:text "xyz"}]

      [{:text "asdf{%1"} {:text "23"} {:open "A"} {:text "%"} {:close "A"} {:text "}gh"}]
      [{:text "asdf"} {:action "123"} {:open "A"} {:close "A"} {:text "gh"}]

      ;; the first action ends with a character from the closing sequence
      [{:text "asdf{%1234%"} {:open "X"} {:text "}ghi"}]
      [{:text "asdf"} {:action "1234"} {:open "X"} {:text "ghi"}]

      [{:text "asdf{"} {:open "!"} {:text "%1234"} {:close "!"} {:text "%}"}]
      [{:text "asdf"} {:action "1234"} {:open "!"} {:close "!"}]

      [{:text "asdf{%1234"} {:text "56%"} {:text "}ghi"}]
      [{:text "asdf"} {:action "123456"} {:text "ghi"}]))

  (testing "Complex case"
    (are [x expected] (= expected (cleanup-runs x))
      [{:text "a{"} {:text "%"} {:text "="} {:text "1"} {:text "%"} {:text "}b"}]
      [{:text "a"} {:action "=1"} {:text "b"}]))

  (testing "Unchanged"
    (are [x expected] (= expected (cleanup-runs x))
      [{:text "asdf{"} {:text "{aaa"}]
      [{:text "asdf{{aaa"}])))

(deftest cleanup-runs-test-redefined-tags
  (with-redefs [stencil.types/open-tag "{{"
                stencil.types/close-tag "}}"]
    (are [x expected] (= expected (vec (cleanup-runs x)))
      [{:text "asdf{{1234}}ghi"}]
      [{:text "asdf"} {:action "1234"} {:text "ghi"}])))

(defmacro are+ [argv [& exprs] & bodies] (list* 'do (for [e exprs] `(are ~argv ~e ~@bodies))))

(def O1 {:open 1})
(def O2 {:open 2})
(def O3 {:open 3})
(def O4 {:open 4})
(def O5 {:open 5})

(deftest cleanup-runs_fragments-only
  (testing "text token has full expression"
    (with-redefs [map-action-token map-action-token']
      (are+ [x expected-literal expected-parsed]
            [(= expected-literal (binding [*only-includes* true] (vec (cleanup-runs x))))
             (= expected-parsed (binding [*only-includes* false] (vec (cleanup-runs x))))]

            [{:text "{%=1%}"}]
            [{:text "{%=1%}"}]
            [{:action {:cmd :cmd/echo, :expression 1 :raw "{%=1%}"}}]

            [{:text "{{%=1%}"}]
            [{:text "{{%=1%}"}]
            [{:text "{"} {:action {:cmd :cmd/echo, :expression 1, :raw "{%=1%}"}}]

            [{:text "{a{%=1%}"}]
            [{:text "{a{%=1%}"}]
            [{:text "{a"} {:action {:cmd :cmd/echo, :expression 1, :raw "{%=1%}"}}]

            [{:text "{%=x%2%}"}]
            [{:text "{%=x%2%}"}]
            [{:action {:cmd :cmd/echo, :expression '(:mod x 2), :raw "{%=x%2%}"}}]

            [{:text "abc{%=1%}b"}]
            [{:text "abc{%=1%}b"}]
            [{:text "abc"} {:action {:cmd :cmd/echo, :expression 1 :raw "{%=1%}"}} {:text "b"}]

            [{:text "abc{%="} O1 O2 {:text "1"} O3 O4 {:text "%}b"}]
            [{:text "abc{%="} O1 O2 {:text "1"} O3 O4 {:text "%}b"}]
            [{:text "abc"} {:action {:cmd :cmd/echo, :expression 1 :raw "{%=1%}"}} O1 O2 O3 O4 {:text "b"}]

            [{:text "abc{%="} O1 O2 {:text "1%"} O3 O4 {:text "}b"}]
            [{:text "abc{%="} O1 O2 {:text "1%"} O3 O4 {:text "}b"}]
            [{:text "abc"} {:action {:cmd :cmd/echo, :expression 1 :raw "{%=1%}"}} O1 O2 O3 O4 {:text "b"}]

            [{:text "abcd{%="} O1 {:text "1"} O2 {:text "%"} O3 {:text "}"} O4 {:text "b"}]
            [{:text "abcd{%="} O1 {:text "1"} O2 {:text "%"} O3 {:text "}"} O4 {:text "b"}]
            [{:text "abcd"} {:action {:cmd :cmd/echo, :expression 1 :raw "{%=1%}"}} O1 O2 O3 O4 {:text "b"}]

            [{:text "abc{"} O1 {:text "%"} O2 {:text "=1"} O3 {:text "2"} O4 {:text "%"} O5 {:text "}"} {:text "b"}]
            [{:text "abc{"} O1 {:text "%"} O2 {:text "=1"} O3 {:text "2"} O4 {:text "%"} O5 {:text "}b"}]
            [{:text "abc"} {:action {:cmd :cmd/echo, :expression 12 :raw "{%=12%}"}} O1 O2 O3 O4 O5 {:text "b"}]

            [O1 {:text "{%if p"} O2 O3 {:text "%}one{%end%}"} O4]
            [O1 {:text "{%if p"} O2 O3 {:text "%}one{%end%}"} O4]
            [O1 {:action {:cmd :cmd/if, :condition 'p :raw "{%if p%}"}} O2 O3 {:text "one"} {:action {:cmd :cmd/end :raw "{%end%}"}} O4]))))

(deftest test-unmap-text-nodes
  (let [unmap-text-nodes @#'stencil.merger/unmap-text-nodes]
    (is (= [] (into [] (unmap-text-nodes) [])))
    (is (= [1 2 3] (into [] (unmap-text-nodes) [1 2 3])))
    (is (= [{:text "abc"}] (into [] (unmap-text-nodes) "abc")))
    (is (= [1 2 {:text "bc"} 3 {:text "d"} 4 5 {:text "e"} 6]
           (into [] (unmap-text-nodes) [1 2 \b \c 3 \d 4 5 \e 6])))))
