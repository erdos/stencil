(ns stencil.errors
  (:import [io.github.erdos.stencil.exceptions ParsingException])
  (:require [stencil.types :refer :all]
            [clojure.test :refer [deftest is are testing]]
            [stencil.model :as model]))


(defn- test-prepare [xml-str]
  (->> xml-str (str) (.getBytes) (new java.io.ByteArrayInputStream) (model/->exec)))


(defmacro ^:private throw-ex-info? [expr]
  `(is (~'thrown? clojure.lang.ExceptionInfo (test-prepare ~expr))))


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
    (throw-ex-info? "<a>{%=</a>")
    (throw-ex-info? "<a>{%=x</a>")
    (throw-ex-info? "<a>{%=x%</a>")
    (throw-ex-info? "<a>{%=x}</a>"))
  (testing "Middle expr is not closed"
    (throw-ex-parsing? "<a><b>{%=1%}</b>{%=3<c>{%=4%}</c></a>")))

(deftest test-unexpected-cmd
  (throw-ex-parsing? "<a>{% echo 3 %}</a>"))
