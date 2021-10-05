(ns stencil.log-test
  (:require [stencil.log :as log]
            [clojure.test :refer [deftest testing]]))

(deftest test-logs
  (testing "Test log macros of various arities"
    (doseq [x '[stencil.log/info stencil.log/warn stencil.log/debug stencil.log/trace stencil.log/error]]
      (eval (list x "Message without param"))
      (eval (list x "Message with param: {}" 1))
      (eval (list x "Message with params: {} {}" 1 2)))))
