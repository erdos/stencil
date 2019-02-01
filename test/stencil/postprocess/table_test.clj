(ns stencil.postprocess.table-test
  (:require [stencil.postprocess.table :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [stencil.util :refer [xml-zip]]))



(deftest get-borders-test
  ;; we set up a 2X2 table and query the borders for each side.
  (let [b (fn [dir val] {:tag dir :content [val]})
        tl [(b "left" 1) (b "right" 2) (b "top" 3) (b "bottom" 4)]
        tr [(b "left" 5) (b "right" 6) (b "top" 7) (b "bottom" 8)]
        bl [(b "left" 9) (b "right" 10) (b "top" 11) (b "bottom" 12)]
        br [(b "left" 13) (b "right" 14) (b "top" 15) (b "bottom" 16)]
        ->tc (fn [borders]
               {:tag "tc" :content [{:tag "tcPr" :content [{:tag "tcBorders" :content borders}]}]})
        bordered
        {:tag "tbl"
         :content [{:tag "tr" :content [(->tc tl) (->tc tr)]}
                   {:tag "tr" :content [(->tc bl) (->tc br)]}]}
        table-zip (xml-zip bordered)]

    (testing "Left borders"
      (is (= [{:tag "left" :content [1]} {:tag "left" :content [9]}]
             (get-borders "left" table-zip))))
    (testing "Right borders"
      (is (= [{:tag "right" :content [6]} {:tag "right" :content [14]}]
             (get-borders "right" table-zip))))
    (testing "Top borders"
      (is (= [{:tag "top" :content [3]} {:tag "top" :content [7]}]
             (get-borders "top" table-zip))))
    (testing "Bottom borders"
      (is (= [{:tag "bottom" :content [12]} {:tag "bottom" :content [16]}]
             (get-borders "bottom" table-zip))))))
