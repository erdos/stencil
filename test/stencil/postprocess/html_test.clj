(ns stencil.postprocess.html-test
  (:require [clojure.test :refer [deftest testing is are]]
            [stencil.ooxml :as ooxml]
            [stencil.postprocess.html :refer :all]))

(defn <p> [& contents] {:tag ooxml/p :content contents})
(defn <r> [& contents] {:tag ooxml/r :content contents})
(defn <rPr> [& contents] {:tag ooxml/rPr :content contents})
(defn <t> [& contents] {:tag ooxml/t :content contents})

(deftest test-ooxml-runs
  (testing "Empty input"
    (is (= nil (html->ooxml-runs nil [])))
    (is (= nil (html->ooxml-runs nil ""))))
  (testing "Naive simple input"
    (is (= [{:tag ooxml/r
             :content [{:tag ooxml/rPr :content []}
                       {:tag ooxml/t :content ["Hello Vilag!"]}]}]
           (html->ooxml-runs "Hello Vilag!" []))))
  (testing "One big formatter")
  (testing "Multiple formatters"))

(deftest asd
  (testing "Unchanged"
    (= (<p> "Hajdiho") (fix-html-chunks (<p> "Hajdiho"))))
  (testing "Complicated case"
    (fix-html-chunks
     (<p> (<r>
           (<rPr>)
           (<t> "Elotte1")
           (<t> "Mr " (->HtmlChunk "E<u>rd</u>os") "E2")
           (<t> "Utana"))))))
