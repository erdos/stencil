(ns stencil.postprocess.list-ref-test
  (:require [stencil.postprocess.list-ref :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [stencil.integration :as integration]))


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

(deftest render-number-chicago
  (is (= "*" (render-number "chicago" 1)))
  (is (= "##" (render-number "chicago" 12))))

(deftest test-pattern-rm-prefix-if-no-suffix
  (is (= ".%1/%2/%3." (pattern-rm-prefix-if-no-suffix ".%1/%2/%3.")))
  (is (= "%1/%2/%3"   (pattern-rm-prefix-if-no-suffix "%1/%2/%3.")))
  (is (= "%1/%2/%3"   (pattern-rm-prefix-if-no-suffix "%1/%2/%3"))))


;; flag h only: text content
;; flag p: only "above" or "below"
;; flag r: "Number", without dot
;; flag n: "Number (No Context)", no dot
;; flag w: "Number (Full Context)", no dot
(deftest render-list-sanity
  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1."}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2."}
                {:start 1 :num-fmt "upperLetter" :lvl-text "%3."}]]
    (is (= "C" (render-list styles {:stack '(3 1 1)} {:flags #{:n :h}} ())))
    (is (= "1.i.C" (render-list styles {:stack '(3 1 1)} {:flags #{:w :h}} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= ".C." (render-list styles {:stack '(3 1 1)} {:flags #{:n :h}} ())))
    (is (= "1-i.C." (render-list styles {:stack '(3 1 1)} {:flags #{:w :h}} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "-%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= "-1-i.C." (render-list styles {:stack '(3 1 1)} {:flags #{:w :h}} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "-%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "/%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= "-1-/i/.C." (render-list styles {:stack '(3 1 1)} {:flags #{:w :h}} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= "1-i.C." (render-list styles {:stack '(3 1 1)} {:flags #{:w :h}} ()))))

  :ok)


(deftest render-list-relative
  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1."}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2."}
                {:start 1 :num-fmt "upperLetter" :lvl-text "%3."}]]
    (is (= "C" (render-list styles {:stack '(3 1 1)} {:flags #{:r :h}} '(3 1 1))))
    (is (= "i.C" (render-list styles {:stack '(3 1 1)} {:flags #{:r :h}} '(1))))
    (is (= "1.i.C" (render-list styles {:stack '(3 1 1)} {:flags #{:r :h}} '(2 2 2 2)))))

  :ok)


(deftest test-parse-instr-text
  (is (= {:flags #{:w :h}
          :id "__RefNumPara__1_1003916714"}
         (parse-instr-text " REF __RefNumPara__1_1003916714 \\w \\h "))))

(deftest test-integration
  (is (= ["Testing cross-reference to numbering with position."
          "Crossref 1: " "2" " " "below"
          "One" "Three"
          "Ein" "Drei"
          "Crossref 2: " "2" " " "above"]
         (integration/rendered-words "references/crossref-numbering-1.docx" {}))))

(deftest test-integration-bookmark
  (is (= ["Test cross-references with bookmarked content."
          "constant" " item"
          "Second item"
          "constant item"]
         (integration/rendered-words "references/crossref-bookmark-1.docx" {:x "constant"}))))
