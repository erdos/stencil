(ns stencil.postprocess.html-test
  (:require [clojure.test :refer [deftest testing is are]]
            [stencil.ooxml :as ooxml]
            [stencil.postprocess.html :refer :all]))

(defn <p> [& contents] {:tag ooxml/p :content (vec contents)})
(defn <r> [& contents] {:tag ooxml/r :content (vec contents)})
(defn <rPr> [& contents] {:tag ooxml/rPr :content (vec contents)})
(defn <t> [& contents] {:tag ooxml/t :content (vec contents)})

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
    (is (=
         (<p>
          (<r> (<rPr>) (<t> "Elotte"))
          (<r> (<rPr>) (<t> "Mr "))
          (<r> (<rPr>) (<t> "E"))
          (<r> (<rPr>
                {:attrs {:xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/val "single"}
                 :tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/u})
               (<t> "rd"))
          (<r> (<rPr>) (<t> "os"))
          (<r> (<rPr>) (<t> "E2"))
          (<r> (<rPr>) (<t> "Utana")))
         (fix-html-chunks
          (<p>
           (<r>
            (<rPr>)
            (<t> "Elotte")
            (<t> "Mr " (->HtmlChunk "E<u>rd</u>os") "E2")
            (<t> "Utana"))))))))
