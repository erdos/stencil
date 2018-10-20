(ns stencil.errors
  (:require [stencil.types :refer :all]
            [clojure.test :refer [deftest is are testing]]
            [stencil.process :refer :all]))

(defn- test-prepare [xml-str data-map]
  (->> xml-str
       str
       .getBytes
       (new java.io.ByteArrayInputStream)
       (prepare-template :xml)))

(defmacro throw-ex-info? [expr]
  `(is (~'thrown? clojure.lang.ExceptionInfo (test-prepare ~expr {}))))

(deftest test-arithmetic-errors
  (testing "Arithmetic errors"
    (throw-ex-info? "<a>{%=%}</a>")
    (throw-ex-info? "<a>{%=a++%}</a>")
    (throw-ex-info? "<a>{%====%}</a>")))
                                        ;
(deftest test-not-closed
  (testing "Expressions are not closed properly"
    (throw-ex-info? "<a>{%=</a>")
    (throw-ex-info? "<a>{%=x</a>")
    (throw-ex-info? "<a>{%=x%</a>")
    (throw-ex-info? "<a>{%=x}</a>"))
  (testing "Middle expr is not closed"
    (throw-ex-info? "<a><b>{%=1%}</b>{%=3<c>{%=4%}</c></a>")))
