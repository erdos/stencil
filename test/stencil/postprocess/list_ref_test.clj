(ns stencil.postprocess.list-ref-test
  (:require [stencil.postprocess.list-ref :refer :all]
            [clojure.zip :as zip]
            [clojure.test :refer [deftest testing is are]]))


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


;; flag h only: text content
;; flag p: only "above" or "below"
;; flag r: "Number", without dot
;; flag n: "Number (No Context)", no dot
;; flag w: "Number (Full Context)", no dot
(deftest render-list-sanity
  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1."}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2."}
                {:start 1 :num-fmt "upperLetter" :lvl-text "%3."}]]
    (is (= "C" (render-list styles '(3 1 1) #{:n :h} ())))
    (is (= "1.i.C" (render-list styles '(3 1 1) #{:w :h} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= ".C." (render-list styles '(3 1 1) #{:n :h} ())))
    (is (= "1-i.C." (render-list styles '(3 1 1) #{:w :h} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "-%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= "-1-i.C." (render-list styles '(3 1 1) #{:w :h} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "-%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "/%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= "-1-/i/.C." (render-list styles '(3 1 1) #{:w :h} ()))))

  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1-"}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2/"}
                {:start 1 :num-fmt "upperLetter" :lvl-text ".%3."}]]
    (is (= "1-i.C." (render-list styles '(3 1 1) #{:w :h} ()))))

  :ok)


(deftest render-list-relative
  (let [styles [{:start 1 :num-fmt "decimal" :lvl-text "%1."}
                {:start 1 :num-fmt "lowerRoman" :lvl-text "%2."}
                {:start 1 :num-fmt "upperLetter" :lvl-text "%3."}]]
    (is (= "C" (render-list styles '(3 1 1) #{:r :h} '(3 1 1))))
    (is (= "i.C" (render-list styles '(3 1 1) #{:r :h} '(1))))
    (is (= "1.i.C" (render-list styles '(3 1 1) #{:r :h} '(2 2 2 2)))))

  :ok)


(deftest test-parse-instr-text
  (is (= {:flags #{:w :h}
          :id "__RefNumPara__1_1003916714"}
         (parse-instr-text " REF __RefNumPara__1_1003916714 \\w \\h "))))
