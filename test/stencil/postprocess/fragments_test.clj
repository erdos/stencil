(ns stencil.postprocess.fragments-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.zip :as zip]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer [find-first-in-tree xml-zip]]
            [stencil.postprocess.fragments :refer :all]))

(defn- p [& xs] {:tag ooxml/p :content (vec xs)})
(defn- r [& xs] {:tag ooxml/r :content (vec xs)})
(defn- t [& xs] {:tag ooxml/t :content (vec xs)})

(def -split-paragraphs @#'stencil.postprocess.fragments/split-paragraphs)

(deftest test-split-paragraphs-1
  (let [data {:tag :root :content [(p (r (t "hello" :HERE) (t "vilag")))]}
        loc (find-first-in-tree #{:HERE} (xml-zip data))]
    (-> loc
        (doto assert)
        (-split-paragraphs (p (r (t "one"))) (p (r (t "two"))))
        (zip/root)
        (= {:tag :root
             :content [(p (r (t "hello"))) ;; before
                       (p (r (t "one"))) (p (r (t "two"))) ;; newly inserted
                       (p (r (t) (t "vilag")))]}) ;; after
        (is))))