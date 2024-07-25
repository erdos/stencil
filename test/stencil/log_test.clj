(ns stencil.log-test
  (:require [stencil.log]
            [clojure.test :refer [deftest testing is]]))

(deftest test-logs
  (testing "Test log macros of various arities"
    (doseq [x '[stencil.log/info
                stencil.log/warn
                stencil.log/debug
                stencil.log/trace
                stencil.log/error]
            c [["message without params"]
               ["message with param: {}" 1]
               ["message with params: {} {}" 1 2]]]
      (eval (list* x c))
      (is true))))
