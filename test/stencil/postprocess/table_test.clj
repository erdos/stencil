(ns stencil.postprocess.table-test
  (:require [stencil.postprocess.table :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [stencil.util :refer [xml-zip]]))


(deftest get-borders-test
  (let [borders [{:tag "left" :content [::left-border]}
                 {:tag "right" :content [::right-border]}]
        bordered
        {:tag "tbl"
         :content [{:tag "tr" :content [{:tag "tc" :content [{:tag "tcPr" :content [{:tag "tcBorders" :content borders}]}]}]}]}]
    (testing "Left borders"
      (is (= [{:tag "left" :content [::left-border]}] (get-borders "left" (xml-zip bordered))))
      (is (= [{:tag "right" :content [::right-border]}] (get-borders "right" (xml-zip bordered)))))))
