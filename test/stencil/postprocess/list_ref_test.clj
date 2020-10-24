(ns stencil.postprocess.list-ref-test
  (:require [stencil.postprocess.list-ref :refer :all]
            [clojure.zip :as zip]
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

(deftest render-number-upper-roman
  (is (= "I" (render-number "upperRoman" 1)))
  (is (= "IX" (render-number "upperRoman" 9)))
  (is (= "LVI" (render-number "upperRoman" 56))))

(deftest test-pattern-rm-prefix-if-no-suffix
  (is (= ".%1/%2/%3." (pattern-rm-prefix-if-no-suffix ".%1/%2/%3.")))
  (is (= "%1/%2/%3"   (pattern-rm-prefix-if-no-suffix "%1/%2/%3.")))
  (is (= "%1/%2/%3"   (pattern-rm-prefix-if-no-suffix "%1/%2/%3"))))

#_
(deftest render-list-sanity
  (is (= "2(5)" (render-list
                 [{:start 1 :num-fmt "decimal" :lvl-text "%1."}
                  {:start 1 :num-fmt "decimal" :lvl-text "%1(%2)"}]
                 [2 5]
                 #{:r :h}))))

;; flag h only: text content
;; flag p: only "above" or "below"
;; flag r: "Number", without dot
;; flag n: "Number (No Context)", no dot
;; flag w: "Number (Full Context)", no dot
(deftest render-list-sanity
  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1."}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2."}
                {:start 1 :num-fmt "upperLetter" :lvl-text "%3."}]]
    (is (= "C" (render-list styles [1 1 3] #{:r :h} ())))
    (is (= "C" (render-list styles [1 1 3] #{:n :h} ())))
    (is (= "1.i.C" (render-list styles [1 1 3] #{:w :h} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= ".C." (render-list styles [1 1 3] #{:r :h} ())))
    (is (= ".C." (render-list styles [1 1 3] #{:n :h} ())))
    (is (= "1-i.C." (render-list styles [1 1 3] #{:w :h} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "-%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= "-1-i.C." (render-list styles [1 1 3] #{:w :h} ()))))
        ;; "-1-i/.C."

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "-%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "/%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= "-1-/i/.C." (render-list styles [1 1 3] #{:w :h} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= "1-i.C." (render-list styles [1 1 3] #{:w :h} ()))))

  :ok)


#_
(deftest
  (is (= " REF __RefNumPara__1_1003916714 \\w \\h "
         (instr-text-ref {:tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/instrText
                          :contents [" REF __RefNumPara__1_1003916714 \\w \\h "]}))))

(deftest test-parse-instr-text
  (is (=
       {:flags #{:w :h}
        :id "__RefNumPara__1_1003916714"}
       (parse-instr-text " REF __RefNumPara__1_1003916714 \\w \\h "))))

(deftest find-elem-test
  )
