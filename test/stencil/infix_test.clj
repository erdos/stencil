(ns stencil.infix-test
  (:import [clojure.lang ExceptionInfo])
  (:require [stencil.infix :as infix :refer :all]
            [stencil.types :refer [hide-table-column-marker?]]
            [clojure.test :refer [deftest testing is are]]))

(defn- run
  ([xs] (run xs {}))
  ([xs args] (infix/eval-rpn args (infix/parse xs))))

(deftest tokenize-test
  (testing "simple fn call"
    (is (= [(->FnCall "sin") 1 :plus 2 :close] (infix/tokenize "   sin(1+2)"))))

  (testing "comma"
    (is (= [:open 1 :comma 2 :comma 3 :close] (infix/tokenize "  (1,2   ,3)   ")))))

(deftest test-read-string-literal
  (testing "Incomplete string"
    (is (thrown? ExceptionInfo (read-string-literal "'alaba")))))

(deftest tokenize-string-literal
  (testing "spaces are kept"
    (is (= [" "] (infix/tokenize " \" \" ")))
    (is (= [" x "] (infix/tokenize " \" x \" "))))
  (testing "special string literals"
    (is (= ["x"] (infix/tokenize "'x'")))
    (is (= ["x"] (infix/tokenize "'x'")))
    (is (= ["x"] (infix/tokenize "‘x’")))
    (is (= ["x"] (infix/tokenize "’x’")))
    (is (= ["x"] (infix/tokenize "„x”"))))
  (testing "escape characters are supported"
    (is (= ["aaa\"bbb"] (infix/tokenize "\"aaa\\\"bbb\"")))))

(deftest tokenize-string-fun-eq
  (testing "tricky"
    (is (= ["1" :eq #stencil.infix.FnCall{:fn-name "str"} 1 :close]
           (infix/tokenize "\"1\" = str(1)")))
    (is (= ["1" 1 {:fn "str" :args 1} :eq]
           (infix/parse "\"1\" = str(1)")))))

(deftest parse-simple
  (testing "Empty"
    (is (thrown? ExceptionInfo (infix/parse nil)))
    (is (thrown? ExceptionInfo (infix/parse ""))))

  (testing "Simple values"
    (is (= [12] (infix/parse "  12 ") (infix/parse "12")))
    (is (= '[ax "y" :get] (infix/parse "   ax.y  "))))

  (testing "Simple operations"
    (is (= [1 2 :plus]
           (infix/parse "1 + 2")
           (infix/parse "1+2    ")
           (infix/parse "1+2")))
    (is (= [3 2 :times] (infix/parse "3*2"))))

  (testing "Parentheses"
    (is (= [3 2 :plus 4 :times]
           (infix/parse "(3+2)*4")))
    (is (= [3 2 :plus 4 1 :minus :times]
           (infix/parse "(3+2)*(4 - 1)")))))

(deftest all-ops-supported
  (testing "Minden operatort vegre tudunk hajtani?"
    (let [ops (-> #{}
                  (into (vals infix/ops))
                  (into (vals infix/ops2))
                  (into (keys infix/operation-tokens))
                  (disj :open :close :comma :open-bracket :close-bracket))
          known-ops (set (filter keyword? (keys (methods @#'infix/reduce-step))))]
      (is (every? known-ops ops)))))

(deftest basic-arithmetic
  (testing "Alap matek"

    (testing "Egyszeru ertekek"
      (is (= 12 (run " 12  "))))

    (testing "Aritmetika"
      (is (= 5 (run "2 +3")))
      (is (= 3 (run "5 - 2")))
      (is (= 3 (run "(3 + 2) - 2")))
      (is (= 3 (run "coalesce(5, 1, 0) - 2")))
      (is (= 3 (run "coalesce(5, 1, 0) - coalesce(2, 1)")))
      (is (= 6 (run "2 * 3")))
      (is (= 3 (run "6 / 2")))
      (is (= 0.33333333M (run "1 / x" {"x" (bigdec 3)}))) ;; infinite expansion error
      (is (= 36.0 (run "6 ^ 2")))
      (is (= 2 (run "12 % 5"))))

    (testing "Simple op precedence"
      (is (= 7 (run "1+2*3")))
      (is (= 4 (run "24/3/2")))
      (is (= 7 (run "2*3+1")))
      (is (= 9 (run "(1+2)*3")))
      (is (= 9 (run "3*(1+2)"))))

    (testing "Power is right-associative"
      (is (= 262144.0 (run "4^3^2"))))

    (testing "Osszehasonlito muveletek"
      (is (true? (run "3 = 3")))
      (is (false? (run "3 == 4")))
      (is (true? (run "3 != 4")))
      (is (false? (run "34 != 34"))))

    (testing "Osszehasonlito muveletek - 2"
      (is (true? (run "3 < 4")))
      (is (false? (run "4 < 2")))
      (is (true? (run "4 > 3")))
      (is (true? (run "3 <= 3")))
      (is (true? (run "34 >= 2"))))))

(deftest basic-string-operations
  (testing "Concatenation of strings"
    (is (= "abcd" (run "'ab' + 'c' + 'd'")))
    (is (= "abc123" (run "'abc' + 1 + 23")))))

(deftest nested-field-access
  (is (= 1 (run "a.b"      {"a" {"b" 1}})))
  (is (= 2 (run "a['b-c']" {"a" {"b-c" 2}})))
  (is (= 3 (run "a[1]"     {"a" {"1" 3}})))
  (is (= 4 (run "a['1']"   {"a" {"1" 4}})))
  (is (= 5 (run "a[(1)]"   {"a" {"1" 5}})))
  (is (= 6 (run "(a)[1]"   {"a" {"1" 6}})))

  (is (= {"a" {"b" 1}} (run "data()" {"a" {"b" 1}})))
  (is (= 6 (run "data()['a-b']" {"a-b" 6})))

  (testing "multiple expressions"
    (is (= 7 (run "a[a[1]+4]" {"a" {"1" 2 "6" 7}})))
    (is (= 8 (run "a[1][2]" {"a" {"1" {"2" 8}}}))))

  (testing "mixed lookup"
    (is (= 7 (run "a['x'].y" {"a" {"x" {"y" 7}}})))
    (is (= 8 (run "a.x['y']" {"a" {"x" {"y" 8}}}))))

  (testing "syntax error"
    (is (thrown? ExceptionInfo (run "[3]" {})))
    (is (thrown? ExceptionInfo (run "a[[3]]" {})))
    (is (thrown? ExceptionInfo (run "a[1,2]" {}))))

  (testing "key is missing from input"
    (is (= nil (run "a[1]"     {"a" {"2" 2}})))
    (is (= nil (run "a[1]"     {}))))

  (testing "array access"
    (is (= \t (run "a[1]"     {"a" "stencil"})))
    (is (= nil (run "a['1']"  {"a" "stencil"})))
    (is (= "y" (run "a[1]"    {"a" (new java.util.ArrayList ["x" "y" "z"])})))
    (is (= nil (run "a['1']"  {"a" (new java.util.ArrayList ["x" "y" "z"])})))
    (is (= nil (run "a[1]"    {"a" []})))
    (is (= nil (run "a['1']"  {"a" ["x" "y" "z"]})))
    (is (= "y" (run "a[1]"    {"a" ["x" "y" "z"]})))))

(deftest logical-operators
  (testing "Mixed"
    (is (true? (run "3 = 3 && 4 == 4"))))

  (testing "Negation"
    (is (false? (run "!a" {"a" 1})))
    (is (true? (run "!a" {"a" false})))
    (is (true? (run "!a" {})))
    (testing "Double negation"
      (is (false? (run "!!a" {"a" nil})))
      (is (true? (run "!!a" {"a" true})))))

  (testing "Logical AND"
    (testing "Short form"
      (is (= 2 (run "a & b" {"a" 1 "b" 2})))
      (is (nil? (run "a & b" {"a" 1 "b" nil})))
      (is (nil? (run "a & b" {"b" 2})))
      (is (false? (run "a & b" {"a" false}))))
    (testing "Long form"
      (is (= 2 (run "a && b" {"a" 1 "b" 2})))
      (is (nil? (run "a && b" {"a" 1 "b" nil})))
      (is (nil? (run "a && b" {"b" 2})))
      (is (false? (run "a && b" {"a" false})))))

  (testing "Logical OR"
    (testing "Short form"
      (is (= 1 (run "a | b" {"a" 1 "b" 2})))
      (is (= 1 (run "a | b" {"a" 1 "b" nil})))
      (is (= 2 (run "a | b" {"b" 2})))
      (is (nil? (run "a | b" {"a" false}))))
    (testing "Long form"
      (is (= 1 (run "a || b" {"a" 1 "b" 2})))
      (is (= 1 (run "a || b" {"a" 1 "b" nil})))
      (is (= 2 (run "a || b" {"b" 2})))
      (is (nil? (run "a || b" {"a" false}))))))

(deftest operator-precedeces
  (testing "Operator precedencia"
    (is (= 36 (run "2*3+5*6"))))

  (testing "Operator precedencia - tobb tagu"
    (is (= 36 (run "2*(1+1+1) + (7 - 2) * 6")))
    (is (= 16 (run "6 + (5)*2")))
    (is (= 22 (run "2*( 1+5) + (5)*2")))
    (is (= 7 (run "(2-1)*2+(7-2)"))))

  (testing "Advanced operator precedences"
    ;; https://gist.github.com/PetrGlad/1175640#gistcomment-876889
    (is (= 21 (run "1+((2+3)*4)")))))

(deftest negativ-szamok
  (is (= -123 (run " -123")))
  (is (= 4.123 (run " 4.123")))
  (is (= -6 (run "-3*2")))
  (is (= -6 (run "2*-3")))
  (is (= -6 (run "2*-(3)")))
  (is (= -6 (run "(2)*-3")))
  (is (= -6 (run "2*(-3)")))
  (testing "a minusz jel precedenciaja nagyon magas"
    (is (= 20 (run "10/-1*-2 ")))))

(deftest range-function
  (testing "Wrong arity calls"
    ;; fontos, hogy nem csak tovabbhivunk a range fuggvenyre,
    ;; mert az vegtelen szekvenciat eredmenyezne.
    (is (thrown? ExceptionInfo (run "range()")))
    (is (thrown? ExceptionInfo (run "range(1,2,3,4)"))))

  (testing "fn to create a number range"
    (is (= [0 1 2 3 4] (run "range(5)")))
    (is (= [1 2 3 4] (run "range (1,5)")))
    (is (= [1 3 5] (run "range( 1, 6, 2)")))))

(deftest unknown-function
  (is (thrown? IllegalArgumentException (run "nosuchfunction(1)"))))

(deftest length-function
  (testing "Simple cases"
    (is (= 2 (run "length(\"ab\")"))))
  (testing "Simple cases"
    (is (= true (run "length(\"\")==0")))
    (is (= true (run "1 = length(\" \")")))))

(deftest contains-function
  (is (= true (run "contains(\"red\", vals)"
                   {"vals" ["red" "green" "blue"]})))
  (is (= false (run "contains(\"yellow\", vals)"
                    {"vals" ["red" "green" "blue"]}))))

(deftest sum-function
  (is (= 123.45 (run "sum(vals)", {"vals" [100 20 3 0.45]})))
  (is (= 17 (run "sum(vals)", {"vals" [17]})))
  (is (= 0 (run "sum(vals)", {"vals" []}))))

(deftest coalesce-function
  (is (= 42 (run "coalesce(x, 0)" {"x" 42})))
  (is (= nil (run "coalesce(\"\", \"\")")))
  (is (= "a" (run "coalesce(\"\", \"\", \"a\", \"b\")"))))

(deftest map-function
  (testing "Works for both keyword an string keys"
    (is (= [1 2 3]
           (run "map(\"x\", vals)"
             {"vals" [{"x" 1}
                      {"x" 2}
                      {"y" -1}
                      {:x 3}]}))))
  (testing "Calling in nil results in empty sequence"
    (is (= []
           (run "map(\"x\", vals)" {"vals" nil}))))
  (testing "Nested map"
    (is (= [1 2]
           (run "map('x.y', vals)"
             {"vals" [{:x {:y 1}} {:x {}} {:x {:y 2}}]}))))
  (testing "Nested vectors"
    (is (= [1 2 3]
           (run "map('x..y', vals)"
             {"vals" [{:x [{:y 1} {:y 2}]}
                      {:x [{:y 3}]}]})))
    (is (= [1 2 3]
           (run "map('.x', vals)"
             {"vals" [[{:x 1} {:x 2}] [] [{}] [{:x 3}]]}))))
  (testing "map with sum"
    (is (= 6
           (run "sum(map(\"x\", vals))"
                {"vals" [{"x" 1} {"x" 2} {"x" 3}]})))))



(deftest test-colhide-expr
  (is (hide-table-column-marker? (run "hideColumn()"))))

(deftest test-unexpected
  (is (thrown? ExceptionInfo (parse "aaaa:bbbb"))))

(deftest tokenize-wrong-tokens
  (testing "Misplaced operators and operands"
    (are [x] (thrown? ExceptionInfo (infix/parse x))
      "1++2" "1 2" "1 2 3" "--" "1+" "+ 1" "+-2" "2 3 *" "+*" "/*" "*1*"))
  (testing "Misplaced parentheses"
    (are [x] (thrown? ExceptionInfo (infix/parse x))
      "2)(3" "(23)3" "1(2)" "(2)(3)" ")2" ")" "(" ")(" ")()" "(())" "()" "()()" "1+(1" "(2"))
  (testing "Illegal coma usage"
    (are [x] (thrown? ExceptionInfo (infix/parse x))
      "2,3,4" ",,,,2" ",3" "3," "+,-"))
  (testing "syntax errors should be thrown"
    (are [x] (thrown? ExceptionInfo (infix/parse x))
      "* 1 + 2 4"))
  (testing "syntax errors should be thrown"
    (are [x] (thrown? ExceptionInfo (infix/parse x))
      "* 3 4" "- 1 a" "+ x 2" "/ x y")))
