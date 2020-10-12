(ns stencil.postprocess.list-ref-test
  (:require [stencil.postprocess.list-ref :refer :all]
            [clojure.test :refer [deftest testing is are]]))

;;flags: https://c-rex.net/projects/samples/ooxml/e1/Part4/OOXML_P4_DOCX_REFREF_topic_ID0ESRL1.html#topic_ID0ESRL1
;; r: Inserts the entire paragraph number of the bookmarked paragraph in relative context—or relative to its position in the numbering scheme —without trailing periods.
;; h: creates hyperlink
;; w: Inserts the paragraph number of the bookmarked paragraph in full context from anywhere in the document.

(deftest render-number-decimal
  (is (= "123" (render-number "decimal" 123))))

(deftest render-number-decimal-zero
  (is (= "123" (render-number "decimalZero" 123)))
  (is (= "03" (render-number "decimalZero" 3))))

(deftest render-number-upper-letter
  (is (= "A" (render-number "upperLetter" 1)))
  (is (= "Z" (render-number "upperLetter" 26)))
  (is (= "AA" (render-number "upperLetter" 27)))
  (is (= "AB" (render-number "upperLetter" 28)))
  (is (= "ab" (render-number "lowerLetter" 28))))

(deftest render-list-sanity
  (is (= "2(5)" (render-list
                [{:start 1 :num-fmt "decimal" :lvl-text "%1."}
                 {:start 1 :num-fmt "decimal" :lvl-text "%1(%2)"}]
                [2 5]
                #{:r :h}))))

#_
(deftest
  (is (= " REF __RefNumPara__1_1003916714 \\w \\h "
         (instr-text-ref {:tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/instrText
                          :contents [" REF __RefNumPara__1_1003916714 \\w \\h "]}))))
