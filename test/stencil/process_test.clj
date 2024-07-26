(ns stencil.process-test
  (:require [stencil.types :refer :all]
            [clojure.test :refer [deftest is are testing]]
            [clojure.data.xml :as xml]
            [stencil.eval :as eval]
            [stencil.model :as model]))


(defn- test-prepare [xml-str]
  (->> xml-str (str) (.getBytes) (new java.io.ByteArrayInputStream) (model/->exec)))


(defn- test-eval [xml-str data-map]
  (xml/emit-str (eval/eval-executable (test-prepare xml-str)  data-map {})))


(defmacro ^:private test-equals [expected input data]
  `(is (= (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" ~expected)
          (test-eval ~input ~data))))


(deftest simple-substitution
  (test-equals "<a>3</a>" "<a>{%=x%}</a>" {"x" 3}))


(deftest test-preparing-template-fragment-invocation
  (testing "Snippet contains a fragment invocation"
    (is (= {:dynamic? true,
            :executable [{:open :a}
                         {:open :b}
                         {:stencil.cleanup/blocks [], :cmd :cmd/include, :name "elefant" :raw "{%include \"elefant\"%}"}
                         {:close :b}
                         {:close :a}],
            :fragments #{"elefant"}
            :variables ()}
           (test-prepare "<a><b>{%include \"elefant\"%}</b></a>"))))
  (testing "Fragment invocation is a dynamic expression"
    (is (= {:dynamic? true,
            :executable [{:open :a}
                         {:stencil.cleanup/blocks [], :cmd :cmd/include, :name 'valtozo :raw "{%include valtozo%}"}
                         {:close :a}],
            :fragments #{}
            :variables '("valtozo")}
           (test-prepare "<a>{%include valtozo%}</a>")))))
