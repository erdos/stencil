(ns stencil.postprocess.list-ref-test
  (:require [stencil.postprocess.list-ref :refer :all]
            [clojure.test :refer [deftest testing is are]]))

;;flags: https://c-rex.net/projects/samples/ooxml/e1/Part4/OOXML_P4_DOCX_REFREF_topic_ID0ESRL1.html#topic_ID0ESRL1
;; r: Inserts the entire paragraph number of the bookmarked paragraph in relative context—or relative to its position in the numbering scheme —without trailing periods.
;; r: creates hyperlink

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
                #{:r :h})))
  )
