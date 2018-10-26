(ns stencil.postprocess.html-test
  (:require [clojure.zip :as zip]
            [clojure.data.xml :as xml]
            [stencil.postprocess.html :refer :all]))

(defn <p> [& contents] {:tag ooxml-p :content contents})
(defn <r> [& contents] {:tag ooxml-r :content contents})
(defn <rPr> [& contents] {:tag ooxml-rPr :content contents})

(defn <t> [& contents] {:tag ooxml-t :content contents})

(deftest asdfff
  (let [test-text "<span>Hello <u>Vilag!</u> Mi a <b>helyzet<i>?</i></b></span>"]
    (html->ooxml-runs test-text [])))

(deftest asd
  (fix-html-chunks
   (<p> (<r>
         (<rPr>)
         (<t> "Elotte1")
         (<t> "Mr " (->HtmlChunk "E<u>rd</u>os") "E2")
         (<t> "Utana"))))
  )
