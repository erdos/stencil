(ns stencil.perf
  (:require [stencil.api :as api]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn- stats [numbers]
  (let [n     (count numbers)
        mean  (double (/ (double (reduce + 0.0 numbers)) n))
        stdev (Math/sqrt (/ ^double (reduce + 0.0 (for [a numbers] (Math/pow (- mean ^double a) 2))) (dec n)))]
    [mean stdev]))

(defn- measured [f]
  (let [before (System/nanoTime)]
    (f)
    (quot (double (- (System/nanoTime) before)) 1000000)))

(defn stats-of [n f] (stats (repeatedly n #(measured f))))

(defn black-hole []
  (proxy [java.io.FilterOutputStream] [nil]
    (flush []) (close []) (write ([_ _ _] nil) ([_] nil))))

(def data {"value" "hello world 99"})

(defn main [n template]
  (let [n (Long/parseLong (str n))
        [mean stdev] (stats-of n (partial api/prepare template))]
    (println "Ran" n "times:")
    (println :mean mean :stdev stdev)
    (let [prepared (api/prepare template)
          [mean stdev] (stats-of n #(api/render! template data :output (black-hole)))]
      (println "Rendering:")
      (println :mean mean :stdev stdev))))
