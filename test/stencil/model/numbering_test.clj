(ns stencil.model.numbering-test
  (:require [stencil.model.numbering :refer :all]
            [stencil.ooxml :as ooxml]
            [clojure.test :refer [deftest testing is are]]))

(defn hiccup [body]
  (assert (vector? body))
  (let [[tag attrs & children]
        (if (map? (second body)) body (list* (first body) {} (next body)))]
    {:tag tag, :attrs attrs, :content (mapv hiccup children)}))

(deftest test-style-for-def-empty
  (binding [*numbering* {:parsed (prepare-numbering-xml {:tag :numbering :content []})}]
    (is (= nil (style-def-for "id-1" 2)))))

(deftest test-style-for-def-with-abstract
  (binding [*numbering*
            {:parsed
             (prepare-numbering-xml
              (hiccup
               [:numbering
                [ooxml/tag-abstract-num {ooxml/xml-abstract-num-id "a1"}
                 [ooxml/tag-lvl {ooxml/attr-ilvl "2"}
                  [:start {ooxml/val "1"}]
                  [:numFmt {ooxml/val "none"}]
                  [:suff {ooxml/val "nothing"}]
                  [:lvlText {ooxml/val ""}]
                  [:lvlJc {ooxml/val "start"}]]]
                [ooxml/tag-num {ooxml/attr-numId "id-1"}
                 [ooxml/xml-abstract-num-id {ooxml/val "a1"}]]]))}]
    (is (= {:lvl-text "", :num-fmt "none", :start 1}
           (style-def-for "id-1" 2)))))


(deftest test-style-for-def
  (binding [*numbering*
            {:parsed
             (prepare-numbering-xml
              (hiccup
               [:numbering
                [ooxml/tag-num {ooxml/attr-numId "id-1"}
                 [ooxml/tag-lvl {ooxml/attr-ilvl "2"}
                  [:start {ooxml/val "1"}]
                  [:numFmt {ooxml/val "none"}]
                  [:suff {ooxml/val "nothing"}]
                  [:lvlText {ooxml/val ""}]
                  [:lvlJc {ooxml/val "start"}]]]]))}]
    (is (= {:lvl-text "", :num-fmt "none", :start 1}
           (style-def-for "id-1" 2)))))
