(ns stencil.ignored-tag-test
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.test :refer [deftest is are testing]]
            [stencil.tokenizer :as tokenizer]
            [stencil.merger :as merger]
            [stencil.postprocess.ignored-tag :refer :all]))

;; make all private maps public!
(let [target (the-ns 'stencil.postprocess.ignored-tag)]
  (doseq [[k v] (ns-map target)
          :when (and (var? v) (= target (.ns ^clojure.lang.Var v)))]
    (eval `(defn ~(symbol (str "-" k)) [~'& args#] (apply (deref ~v) args#)))))

(deftest with-pu-test
  (testing "The xmlns alias is inserted despite element not being used."
    (is (= "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Elephant xmlns:b=\"a\"/>"
           (xml/emit-str (-with-pu {:tag "Elephant"} {"a" "b"}))))))

(def test-data-2
  (str "<?xml version='1.0' encoding='UTF-8'?>"
       "<a:document xmlns:a=\"ns1\"
                    xmlns:bb=\"ns2\"
                    xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\">"
       "<a:x mc:Ignorable=\"bb\" />"
       "</a:document>"))

(defn clear-all-metas [x] (-postwalk-xml (fn [c] (if (meta c) (with-meta c {}) c)) x))

(deftest test-ignored-tag-2
  (testing
   "The value in the Ignorable tag is mapped so that the namespace it
       references doesn't change."
    (-> test-data-2
        (java.io.StringReader.) (merger/parse-to-tokens-seq) (tokenizer/tokens-seq->document)
        (clear-all-metas) (unmap-ignored-attr) (xml/emit-str) (xml/parse-str)
        (as-> *
              (let [ignorable-value
                    (-> * :content first :attrs
                        :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fmarkup-compatibility%2F2006/Ignorable)
                    ignorable-ns (-> * meta :clojure.data.xml/nss :p->u (get ignorable-value))]
                (is (not (empty? ignorable-value)))
                (is (= "ns2" ignorable-ns)))))))

(def test-data-1
  (str
   "<?xml version='1.0' encoding='UTF-8'?>"
   "<aa:document xmlns:aa=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"
                 xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\"
                 xmlns:gh=\"http://github.com\"
                 xmlns:x=\"http://dbx.hu/1\"
                 x:teszt=\"teszt1\"
                 mc:Ignorable=\"gh x\">"
   "<aa:body><aa:p xmlns:gh=\"http://dbx.hu/2\" gh:x=\"1\" mc:Ignorable=\"gh x\"></aa:p></aa:body></aa:document>"))

(deftest test-ignored-tag-1
  (-> test-data-1
      (java.io.StringReader.)

      (merger/parse-to-tokens-seq)
      (tokenizer/tokens-seq->document)

      (unmap-ignored-attr)
      (xml/emit-str)

     ;; TODO: check here for sth.
))
:ok

(def test-data-3
  (str
   "<?xml version='1.0' encoding='UTF-8'?>"
   "<aa:document xmlns:aa=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"
                 xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\"
                 xmlns:x=\"teszt1\">"
   "<mc:Choice Requires=\"x\"></mc:Choice>"
   "</aa:document>"))

(deftest test-ignored-tag-3
  (-> test-data-3
      (java.io.StringReader.)

      (merger/parse-to-tokens-seq)
      (tokenizer/tokens-seq->document)

      (unmap-ignored-attr)
      (xml/emit-str)

     ;; TODO: check here for sth.
))
