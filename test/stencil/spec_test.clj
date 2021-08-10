(ns stencil.spec-test
  "Instruments all methods for test cases."
  (:require [clojure.spec.test.alpha :as stest]
            [stencil.spec]))

(doseq [k (stest/instrumentable-syms)
        :when (.startsWith (.getNamespace k) "stencil.")]
  (println "Instrumenting" k)
  (stest/instrument k))
