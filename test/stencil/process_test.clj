(ns stencil.process-test
  (:require [stencil.types :refer :all]
            [clojure.test :refer [deftest is are testing]]
            [clojure.data.xml :as xml]
            [stencil.process :refer :all]))

(defn- test-prepare [xml-str]
  (->> xml-str
       (str)
       (.getBytes)
       (new java.io.ByteArrayInputStream)
       (prepare-template :xml)))

(defn- test-eval [xml-str data-map]
  (-> {:template (test-prepare xml-str)
       :data data-map
       :function (fn [& _] (assert false "ERROR"))}
      (do-eval-stream) (:stream) (slurp) (str)))

(defmacro ^:private test-equals [expected input data]
  `(is (= (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" ~expected)
          (test-eval ~input ~data))))

(deftest simple-substitution
  (test-equals "<a>3</a>" "<a>{%=x%}</a>" {"x" 3}))

(deftest test-preparing-template
  (is (= {:executable [{:open :a} {:open :b} (->close :b) {:open :c} {:close :c} {:close :a}]
          :type :xml, :variables ()}
         (test-prepare "<a><b>{%fragment \"elefant\"%}</b>Elem<c>{%end%}</c></a>"))))
