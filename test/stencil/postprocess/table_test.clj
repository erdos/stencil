(ns stencil.postprocess.table-test
  (:require [stencil.postprocess.table :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [stencil.util :refer [xml-zip]]))


(deftest get-borders-test
  (let [b (fn [dir val] {:tag dir :content [val]})
        borders [(b "left" ::left-border)
                 (b "right" ::right-border)
                 (b "top" ::top-border)
                 (b "bottom" ::bottom-border)]
        borders2 [(b "left" ::left-border-2)
                 (b "right" ::right-border-2)
                 (b "top" ::top-border-2)
                 (b "bottom" ::bottom-border-2)]
        bordered
        {:tag "tbl"
         :content [{:tag "tr" :content [{:tag "tc" :content [{:tag "tcPr" :content [{:tag "tcBorders" :content borders}]}]}]}
                   {:tag "tr" :content [{:tag "tc" :content [{:tag "tcPr" :content [{:tag "tcBorders" :content borders2}]}]}]}]}
        table-zip (xml-zip bordered)]

    (testing "Left borders"
      (is (= [{:tag "left" :content [::left-border]}
              {:tag "left" :content [::left-border-2]}]
             (get-borders "left" table-zip))))
    (testing "Right borders"
      (is (= [{:tag "right" :content [::right-border]}
              {:tag "right" :content [::right-border-2]}]
             (get-borders "right" table-zip))))
    (testing "Top borders"
      (is (= [{:tag "top" :content [::top-border]}]
             (get-borders "top" table-zip))))
    (testing "Bottom borders"
      (is (= [{:tag "bottom" :content [::bottom-border-2]}]
             (get-borders "bottom" table-zip))))))
