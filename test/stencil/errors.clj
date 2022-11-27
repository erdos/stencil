(ns stencil.errors
  (:import [io.github.erdos.stencil.exceptions ParsingException EvalException])
  (:require [stencil.types :refer :all]
            [stencil.integration :refer [test-fails]]
            [clojure.test :refer [deftest is are testing]]
            [stencil.model :as model]))


(defn- test-prepare [xml-str]
  (->> xml-str (str) (.getBytes) (new java.io.ByteArrayInputStream) (model/->exec)))


(defmacro ^:private throw-ex-parsing? [expr]
  `(is (~'thrown? ParsingException (test-prepare ~expr))))


(deftest test-arithmetic-errors
  (testing "Arithmetic errors"
    (throw-ex-parsing? "<a>{%=%}</a>")
    (throw-ex-parsing? "<a>{%=a++%}</a>")
    (throw-ex-parsing? "<a>{%====%}</a>")))


(deftest test-unexpected-else
  (testing "Unexpected else tag"
    (throw-ex-parsing? "<a>{%else%}</a>"))
  (testing "Too many else tags"
    (throw-ex-parsing? "<a>{%if 1%}a{%else%}2{%else%}3{%end%}</a>"))
  (testing "Unexpected else tag in loop"
    (throw-ex-parsing? "<a>{%for x in xs%}a{%else%}2{%end%}</a>"))
  (testing "Unexpected elseif tag in loop"
    (throw-ex-parsing? "<a>{%for x in xs%}a{%elseif y%}2{%end%}</a>")))


(deftest test-missing-end
  (testing "Missing after if."
    (throw-ex-parsing? "<a>{%if 1%}a</a>"))
  (testing "Missing after else."
    (throw-ex-parsing? "<a>{%if 1%}a{%else%}2</a>"))
  (testing "Missing end after elseif."
    (throw-ex-parsing? "<a>{%if 1%}a{%elseif y%}2</a>"))
  (testing "Missing after for."
    (throw-ex-parsing? "<a>{%for x in xs%}a</a>")))


(deftest test-wrong-include
  (testing "Unexpected value in inlude tag"
    (throw-ex-parsing? "<a>{% include header %}</a>")
    (throw-ex-parsing? "<a>{% include a+1 %}</a>")))


(deftest test-not-closed
  (testing "Expressions are not closed properly"
    (throw-ex-parsing? "<a>{%=</a>")
    (throw-ex-parsing? "<a>{%=x</a>")
    (throw-ex-parsing? "<a>{%=x%</a>")
    (throw-ex-parsing? "<a>{%=x}</a>"))
  (testing "Middle expr is not closed"
    (throw-ex-parsing? "<a><b>{%=1%}</b>{%=3<c>{%=4%}</c></a>")))


(deftest test-unexpected-cmd
  (throw-ex-parsing? "<a>{% echo 3 %}</a>"))

;; integration tests

(deftest test-parsing-errors
  (testing "Closing tag is missing"
    (test-fails "test-resources/failures/test-syntax-nonclosed.docx" nil
                ParsingException "Missing {%end%} tag from document!"))
  (testing "Extra closing tag is present"
    (test-fails "test-resources/failures/test-syntax-closed.docx" nil
                ParsingException "Too many {%end%} tags!"))
  (testing "A tag not closed until the end of document"
    (test-fails "test-resources/failures/test-syntax-incomplete.docx" nil
                ParsingException "Stencil tag is not closed. Reading {% if x + y"))
  (testing "Unexpected {%else%} tag"
    (test-fails "test-resources/failures/test-syntax-unexpected-else.docx" nil
                ParsingException "Unexpected {%else%} tag, it must come right after a condition!"))
  (testing "Unexpected {%else if%} tag"
    (test-fails "test-resources/failures/test-syntax-unexpected-elif.docx" nil
                ParsingException "Unexpected {%else if%} tag, it must come right after a condition!"))
  (testing "Cannot parse infix expression"
    (test-fails "test-resources/failures/test-syntax-fails.docx" nil
                ParsingException "Invalid stencil expression!"))
  (testing "Test unexpected command"
    (test-fails "test-resources/failures/test-syntax-unexpected-command.docx" nil
                ParsingException "Unexpected command: unexpected")))

(deftest test-evaluation-errors
  (testing "Division by zero"
    (test-fails "test-resources/failures/test-eval-division.docx" {:x 1 :y 0}
                EvalException "Error evaluating expression: {%=x/y%}"
                java.lang.ArithmeticException "Divide by zero"))
  (testing "NPE"
    (test-fails "test-resources/failures/test-eval-division.docx" {:x nil :y nil}
                EvalException "Error evaluating expression: {%=x/y%}"
                java.lang.NullPointerException nil #_"Cannot invoke \"Object.getClass()\" because \"x\" is null"))
  (testing "function does not exist"
    (test-fails "test-resources/failures/test-no-such-fn.docx" {}
                EvalException "Error evaluating expression: {%=nofun()%}"
                java.lang.IllegalArgumentException "Did not find function for name nofun"))
  (testing "function invoked with wrong arity"
    (test-fails "test-resources/failures/test-syntax-arity.docx" {}
                EvalException "Error evaluating expression: {%=decimal(1,2,3)%}"
                clojure.lang.ExceptionInfo "Function 'decimal' was called with a wrong number of arguments (3)"))
  (testing "Missing fragment"
    (test-fails "test-resources/multipart/main.docx" {}
                EvalException "No fragment for name: body"))
  (testing "date() function has custom message"
    (test-fails "test-resources/test-function-date.docx" {"date" "2022-01-04XXX11:22:33"}
                EvalException "Error evaluating expression: {%=date(\"yyyy-MM-dd\", date)%}"
                IllegalArgumentException "Could not parse date object 2022-01-04XXX11:22:33"))

  (testing "function invocation error"
    ;; TODO: invoke fn with wrong types
  ))