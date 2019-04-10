(ns stencil.postprocess.whitespaces-test
  (:require [stencil.types :refer :all]
            [clojure.test :refer [deftest is are testing]]
            [clojure.data.xml :as xml]
            [stencil.eval :as eval]
            [stencil.process :refer :all]
            [stencil.model :as model]))


(defn- test-eval [xml-str data-map]
  (let [input (->> xml-str
                   str
                   .getBytes
                   (new java.io.ByteArrayInputStream))
        part (model/->exec input)
        evaled-xml (eval/eval-executable part data-map {})]
    (xml/emit-str evaled-xml)))

(defmacro ^:private test-equals [expected input data]
  `(is (= (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" ~expected)
          (test-eval ~input ~data))))

(deftest test-whitespaces
  (testing "xml space preserve is inserted for second <t> tag."
    (test-equals
     "<a:a xmlns:a=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><a:t>Sum: 1</a:t><a:t xml:space=\"preserve\"> pieces</a:t></a:a>"
     "<x:a xmlns:x=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><x:t>Sum: {%=x </x:t><x:t>%} pieces</x:t></x:a>"
     {"x" 1}))
  (testing "existing space=preserve attributes are kept intact"
    (test-equals
     "<a:a xmlns:a=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" xml:space=\"preserve\"> Hello </a:a>"
     "<x:a xmlns:x=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" xml:space=\"preserve\"> Hello </x:a>"
     {})))

;; (test-eval "<a>Sum:<b> {%=x </b>%} pieces</a>" {"x" 1})
