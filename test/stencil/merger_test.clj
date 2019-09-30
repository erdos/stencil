(ns stencil.merger-test
  (:require [stencil.merger :refer :all]
            [clojure.test :refer [deftest testing is are use-fixtures]]))

(use-fixtures :once (fn [f] (with-redefs [action-maybe-parsed identity] (f))))

(deftest peek-next-text-test
  (testing "Simple case"
    (is (= nil (peek-next-text nil)))
    (is (= nil (peek-next-text [])))
    (is (= nil (peek-next-text [{:open 1} {:open 2} {:close 2}])))
    (is (= '({:char \a, :stack nil, :text-rest (\b), :rest ({:text "cd"})}
             {:char \b, :stack nil, :text-rest nil, :rest ({:text "cd"})}
             {:char \c, :stack nil, :text-rest (\d), :rest nil}
             {:char \d, :stack nil, :text-rest nil, :rest nil})
           (peek-next-text [{:text "ab"} {:text "cd"}])))))

(deftest find-first-code-test
  (testing "Simple cases"
    (are [x res] (is (= res (find-first-code x)))
      "asdf{%xy%}gh" {:action "xy" :before "asdf" :after "gh"}
      "{%xy%}gh"     {:action "xy" :after "gh"}
      "asdf{%xy%}"   {:action "xy" :before "asdf"}
      "{%xy%}"       {:action "xy"}
      "a{%xy"        {:action-part "xy" :before "a"}
      "{%xy"         {:action-part "xy"})))

(deftest text-split-tokens-test
  (testing "Simple cases"
    (are [x expected] (is (= expected (text-split-tokens x)))

      "a{%a%}b{%d"
      {:tokens [{:text "a"} {:action "a"} {:text "b"}] :action-part "d"}

      "{%a%}{%x%}"
      {:tokens [{:action "a"} {:action "x"}]}

      ""
      {:tokens []})))

(deftest cleanup-runs-test
  (testing "Simple cases"
    (are [x expected] (= expected (cleanup-runs x))
      [{:text "{%1234%}"} {:text "{%a%}{%b%}"}]
      [{:action "1234"} {:action "a"} {:action "b"}]

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
      [{:text "asdf{"} {:text "{aaa"}])))
