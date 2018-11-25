(ns stencil.postprocess.html-test
  (:import [clojure.lang ExceptionInfo])
  (:require [clojure.test :refer [deftest testing is are]]
            [stencil.ooxml :as ooxml]
            [stencil.postprocess.html :refer :all]))

(defn- <p> [& contents] {:tag ooxml/p :content (vec contents)})
(defn- <r> [& contents] {:tag ooxml/r :content (vec contents)})
(defn- <rPr> [& contents] {:tag ooxml/rPr :content (vec contents)})
(defn- <t> [& contents] {:tag ooxml/t :content (vec contents)})
(defn- <br> [] {:tag ooxml/br :content []})

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

(deftest test-fix-html-chunks-errors
  (testing "Invalid HTML content"
    (is (thrown?
         ExceptionInfo
         (fix-html-chunks
          (<p>
           (<r>
            (<rPr>)
            (<t> (->HtmlChunk "<u>Content is not closed."))))))))
  (testing "Unsupported HTML tag"
    (is (thrown?
         ExceptionInfo
         (fix-html-chunks
          (<p>
           (<r>
            (<rPr>)
            (<t> (->HtmlChunk "<illegal>rd</illegal>")))))))))

(deftest test-fix-html-chunks
  (testing "Unchanged"
    (is (= (<p> "Hajdiho") (fix-html-chunks (<p> "Hajdiho")))))
  (testing "Br tags"
    (is (= (<p> (<r> (<rPr>) (<t> "One") (<br>) (<t> "Two")))
           (fix-html-chunks (<p> (<r> (<rPr>) (<t> (->HtmlChunk "One<br>Two"))))))))
  (testing "Complicated case"
    (is (=
         (<p>
          (<r> (<rPr>) (<t> "Elotte1") (<t> "Elotte2"))
          (<r> (<rPr>) (<t> "Mr "))
          (<r> (<rPr>) (<t> "E"))
          (<r> (<rPr>
                {:attrs {:xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/val "single"}
                 :tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/u})
               (<t> "rd"))
          (<r> (<rPr>) (<t> "os"))
          (<r> (<rPr>) (<t> "E2"))
          (<r> (<rPr>) (<t> "Utana1") (<t> "Utana2")))
         (fix-html-chunks
          (<p>
           (<r>
            (<rPr>)
            (<t> "Elotte1")
            (<t> "Elotte2")
            (<t> "Mr " (->HtmlChunk "E<u>rd</u>os") "E2")
            (<t> "Utana1")
            (<t> "Utana2"))))))))
