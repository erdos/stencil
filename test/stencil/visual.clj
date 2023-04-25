(ns stencil.visual
  (:require [clojure.java.shell]
            [clojure.test :refer [deftest testing use-fixtures]]
            [stencil.integration :refer [render-visual-compare!]]))

;; only run tests on CI
(use-fixtures :once
  (fn [f] (when (System/getenv "STENCIL_TEST_VISUAL") (f))))

(deftest test-visual-multipart-1
  (testing "Fragments are imported with custom styles"
    (render-visual-compare!
     :template  "multipart/main.docx"
     :data      {:name "Johnie Doe"}
     :expected  "multipart/expected.pdf"
     :fragments {"body"   "multipart/body.docx"
                 "header" "multipart/header.docx"
                 "footer" "multipart/footer.docx"})))
