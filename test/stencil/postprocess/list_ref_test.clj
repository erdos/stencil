(ns stencil.postprocess.list-ref-test
  (:require [stencil.postprocess.list-ref :refer :all]
            [stencil.util :refer [xml-zip]]
            [clojure.test :refer [deftest testing is]]
            [clojure.zip :as zip]
            [stencil.integration :as integration]))


(declare -find-elem -descendants)


;; make all private maps public!
(let [target (the-ns 'stencil.postprocess.list-ref)]
  (doseq [[k v] (ns-map target)
          :when (and (var? v) (= target (.ns ^clojure.lang.Var v)))]
    (eval `(defn ~(symbol (str "-" k)) [~'& args#] (apply (deref ~v) args#)))))


(deftest test-descendants
  (let [tree (xml-zip {:tag :0
                       :content [{:tag :a :content [{:tag :b}
                                                    {:tag :c :content [{:tag :d}]}
                                                    {:tag :e}]}]})]
    (testing "siblings are not returned"
      (is (= [{:tag :b}]
             (->> tree zip/down zip/down -descendants (map zip/node)))))
    (is (= [:a :b :c :d :e]
           (map (comp :tag zip/node) (-descendants (zip/down tree)))))
    (is (= [1 2 3 4]
           (map zip/node (next (-descendants (xml-zip {:tag :0 :content [1 2 3 4]}))))))))


(deftest test-find-elem
  (let [tree (xml-zip {:tag :a :content [{:tag :b :content [1 2 3]}
                                         {:tag :c :content [4 5 6]}]})]
    (testing "Test by tag name"
      (testing "Find root"
        (is (= (zip/node tree) (zip/node (-find-elem tree :tag :a)))))
      (let [f (-find-elem tree :tag :b)]
        (is (= {:tag :b :content [1 2 3]} (zip/node f))))
      (is (= nil (-find-elem tree :tag :not-found)))
      (is (= nil (-find-elem (zip/down tree) :tag :a))))))


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

(deftest test-integration-bookmark-missing
  (testing "Bookmark referenced after Question1 is missing so its contents will not change."
    (is (= ["First para:"
            "Question 1" "Question 2" "Name: Teszt"
            "Second para:"
            "Lorem ipsum " "Question 1" "." " "
            "Nunc viverra imperdiet enim."
            "Proin pharetra nonummy pede." " " "Question 2"]
           (integration/rendered-words "references/crossref-bookmark-missing.docx" {:name "Teszt"})))))
