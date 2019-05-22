(ns stencil.tree-postprocess-test
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [clojure.test :refer [deftest is are testing]]
            [stencil.util :refer [xml-zip]]
            [stencil.ooxml :as ooxml]
            [stencil.tree-postprocess :refer :all]
            [stencil.postprocess.table :refer :all]))

(defn- table [& contents] {:tag "tbl" :content (vec contents)})
(defn- cell [& contents] {:tag "tc" :content (vec contents)})
(defn- row [& contents] {:tag "tr" :content (vec contents)})
(defn- cell-of-width [width & contents]
  (assert (integer? width))
  {:tag "tc" :content (vec (list* {:tag "tcPr" :content [{:tag "gridSpan" :attrs {:xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/val (str width)}}]} contents))})

(defn tbl-grid [& vals]
  {:tag :tblGrid, :content (for [v vals] {:tag :gridCol :attrs {ooxml/w (str v)}})})

(defn cell-width
  ([w] {:tag :tcPr, :content [{:tag :tcW :attrs {ooxml/val (str w)}}]})
  ([span w] {:tag :tcPr, :content [{:tag :tcW :attrs {ooxml/w (str w)}}
                                   {:tag :gridSpan :attrs {ooxml/val (str span)}}]}))

(defn into-hiccup [c] (if (map? c) (vec (list* (keyword (name (:tag c))) (into {} (:attrs c)) (map into-hiccup (:content c)))) c))

(deftest test-row-hiding-simple
  (testing "Second row is being hidden here."
    (is (=  (table (row (cell "first"))
                   (row (cell "third")))
            (fix-tables (table (row (cell "first"))
                               (row (cell "second" (->HideTableRowMarker)))
                               (row (cell "third"))))))))

(deftest test-column-merging-simple
  (testing "Second column is being hidden here."
    (is (=  (table (row (cell "x1"))
                   (row (cell "d1")))
            (fix-tables (table (row (cell "x1") (cell (->HideTableColumnMarker)))
                               (row (cell "d1") (cell "d2"))))))))

(deftest test-column-merging-joined
  (testing "Second column is being hidden here."
    (is (=  (table (row (cell "x1") (cell "x3"))
                   (row (cell "d1") (cell-of-width 1 "d2")))
            (fix-tables (table (row (cell "x1") (cell (->HideTableColumnMarker) "x2") (cell "x3"))
                               (row (cell "d1") (cell-of-width 2 "d2"))))))))

(deftest test-column-merging-super-complex
  (testing "Second column is being hidden here."
    (is (=
         (table
          (row (cell "H1") (cell "J1"))
          (row (cell-of-width 1 "H2 + I2") (cell "J2"))
          (row (cell "H") (cell-of-width 1 "I3 + J3"))
          (row (cell "H") (cell "J"))
          (row (cell-of-width 2 "F + G + H + I + J")))
         (fix-tables
          (table
           (row (cell-of-width 2 "F1+G1" (->HideTableColumnMarker)) (cell "H1") (cell "I1" (->HideTableColumnMarker)) (cell "J1"))
           (row (cell "F2") (cell "G2") (cell-of-width 2 "H2 + I2") (cell "J2"))
           (row (cell-of-width 2 "F + G") (cell "H") (cell-of-width 2 "I3 + J3"))
           (row (cell "F") (cell "G") (cell "H") (cell "I") (cell "J"))
           (row (cell-of-width 5 "F + G + H + I + J"))))))))

(deftest test-column-merging-super-complex-2
  (testing "Second column is being hidden here."
    (is (=
         (table
          (row (cell "H1") (cell-of-width 2 "J1"))
          (row (cell-of-width 1 "H2 + I2") (cell-of-width 2 "J2"))
          (row (cell "H") (cell-of-width 2 "I3 + J3"))
          (row (cell "H") (cell-of-width 2 "J"))
          (row (cell-of-width 3 "F + G + H + I + J")))
         (fix-tables
          (table
           (row (cell-of-width 2 "F1+G1" (->HideTableColumnMarker)) (cell "H1") (cell-of-width 2 "I1" (->HideTableColumnMarker)) (cell-of-width 2 "J1"))
           (row (cell "F2") (cell "G2") (cell-of-width 3 "H2 + I2") (cell-of-width 2 "J2"))
           (row (cell-of-width 2 "F + G") (cell "H") (cell-of-width 4 "I3 + J3"))
           (row (cell "F") (cell "G") (cell "H") (cell-of-width 2 "I") (cell-of-width 2 "J"))
           (row (cell-of-width 7 "F + G + H + I + J"))))))))

(deftest test-column-merging-super-complex-3
  (testing "Second column is being hidden here."
    (is (=
         (table (row (cell "X1") (cell "X3")) (row (cell "Y1") (cell "Y3")))
         (fix-tables (table (row (cell "X1") (cell-of-width 2 "X2" (->HideTableColumnMarker)) (cell "X3"))
                            (row (cell "Y1") (cell-of-width 2 "Y2") (cell "Y3"))))))))

(deftest test-preprocess-remove-thin-cols
  (testing "There are infinitely thin columns that are being removed."
    (is (=
         (table
          {:tag :tblGrid
           :content [{:tag :gridCol :attrs {:xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/w "2000"}}
                     {:tag :gridCol :attrs {:xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/w "3000"}}]}
          (row (cell "X1") (cell "X3"))
          (row (cell "Y1") (cell "Y3")))
         (fix-tables
          (table
           {:tag :tblGrid
            :content [{:tag :gridCol :attrs {:xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/w "2000"}}
                      {:tag :gridCol :attrs {:xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/w "4"}}
                      {:tag :gridCol :attrs {:xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/w "3000"}}]}
           (row (cell "X1") (cell "X2") (cell "X3"))
           (row (cell "Y1") (cell "Y2") (cell "Y3"))))))))

;; TODO: ennek az egesz tablaatot is at kellene mereteznie!!!!
(deftest test-column-cut
  (testing "We hide second column and expect cells to KEEP size"
    (is (= (table (row (cell-of-width 1 "X1") (cell-of-width 2 "X3"))
                  (row (cell-of-width 1 "Y1") (cell-of-width 2 "Y3")))
           (fix-tables
            (table (row (cell-of-width 1 "X1") (cell-of-width 3 "X2" (->HideTableColumnMarker :cut)) (cell-of-width 2 "X3"))
                   (row (cell-of-width 1 "Y1") (cell-of-width 3 "Y2") (cell-of-width 2 "Y3"))))))))

(deftest resize-cut
  (is (=
       (table {:tag :tblGrid,
               :content [{:tag :gridCol, :attrs {ooxml/w "1000"}}
                         {:tag :gridCol, :attrs {ooxml/w "2000"}}]}
              (row) (row) (row))
       (zip/node
        (table-resize-widths
         (xml-zip (table (tbl-grid 1000 2000 2600 500) (row) (row) (row)))
         :cut
         #{2 3})))))

(deftest resize-last
  (is (=
       (table
        {:tag :tblGrid,
         :content [{:tag :gridCol, :attrs {ooxml/w "1000"}}
                   {:tag :gridCol, :attrs {ooxml/w "5000"}}]}
        (row) (row) (row))
       (zip/node
        (table-resize-widths
         (xml-zip (table (tbl-grid 1000 2000 2500 500) (row) (row) (row)))
         :resize-last
         #{2 3})))))

(deftest resize-first
  (is (=
       (table
        {:tag :tblGrid,
         :content [{:tag :gridCol, :attrs {ooxml/w "4000"}}
                   {:tag :gridCol, :attrs {ooxml/w "2000"}}]}
        (row) (row) (row))
       (zip/node
        (table-resize-widths
         (xml-zip (table (tbl-grid 1000 2000 2500 500) (row) (row) (row)))
         :resize-first
         #{2 3})))))

(deftest resize-rational
  (is (=
       (into-hiccup (table {:tag :tblPr :content [{:tag :tblW, :attrs {ooxml/w "6000"}}]}
                           (tbl-grid 2000 4000)
                           (row (cell (cell-width 1 2000) "a")
                                (cell (cell-width 1 4000) "b"))
                           (row (cell (cell-width 2 6000) "ab"))))

       (into-hiccup (zip/node
                     (table-resize-widths
                      (xml-zip (table {:tag :tblPr
                                       :content [{:tag :tblW :attrs {ooxml/w "?"}}]}
                                      (tbl-grid "1000" "2000" "2500" "500")
                                      (row (cell-of-width 1 "a") (cell-of-width 1 "b"))
                                      (row (cell-of-width 2 "ab"))))

                      :rational
                      #{2 3}))))))

(deftest test-column-hiding-border-right
  (let [border-1 {:tag "tcPr" :content [{:tag "tcBorders" :content [{:tag "right" :attrs {:a 1}}]}]}
        border-2 {:tag "tcPr" :content [{:tag "tcBorders" :content [{:tag "right" :attrs {:a 2}}]}]}]
    (testing "Second column is being hidden here."
      (is (=  (into-hiccup (table (row (cell border-1 "ALMA"))
                                  (row (cell border-2 "NARANCS"))))
              (into-hiccup (fix-tables (table (row (cell "ALMA") (cell border-1 (->HideTableColumnMarker) "KORTE"))
                                              (row (cell "NARANCS") (cell border-2 "BARACK"))))))))))

(deftest test-get-borders
  (is (= '(nil {:attrs {:x 1}, :tag :right})
         (get-borders "right" (xml-zip (table (tbl-grid "1000" "2000" "2500" "500")
                                              (row (cell "a") (cell-of-width 1 "b"))
                                              (row (cell "v") (cell {:tag :tcPr :content [{:tag :tcBorders :content [{:tag :right :attrs {:x 1}}]}]} "dsf"))))))))

:OK
