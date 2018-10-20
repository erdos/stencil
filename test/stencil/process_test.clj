(ns stencil.process-test
  (:require [stencil.types :refer :all]
            [clojure.test :refer [deftest is are testing]]
            [clojure.data.xml :as xml]
            [stencil.process :refer :all]))

(defn- test-eval [xml-str data-map]
  (let [prepared (->> xml-str
                    str
                    .getBytes
                    (new java.io.ByteArrayInputStream)
                    (prepare-template :xml))]
    (-> {:template prepared
        :data data-map
        :function (fn [& _] (assert false "ERROR"))}
       do-eval-stream :stream slurp str)))

(defmacro ^:private test-equals [expected input data]
  `(is (= (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" ~expected)
          (test-eval ~input ~data))))

                                        ; (test-eval "<a> <b> </b> </a>" {})

;; TODO: we should have something for space:preserve notations.

(deftest simple-substitution
  (test-equals "<a>3</a>" "<a>{%=x%}</a>" {"x" 3}))

(deftest test-whitespaces
  (testing "xml space preserve is inserted"
    (test-equals
     "<a>Sum:<b> 1</b> pieces</a>"
     "<a xmlns:x=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">Sum:<x:t> {%=x </x:t>%} pieces</a>"
     {"x" 1})

    #_(test-equals
     "<a>Sum: 1<b xml:space=\"preserve\"> pieces</b></a>"
     "<a>Sum: {%=x <b>%} pieces</b></a>"
     {"x" 1})))

;; (test-eval "<a>Sum:<b> {%=x </b>%} pieces</a>" {"x" 1})
