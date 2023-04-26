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


(deftest test-visual-generative
  (doseq [main '[plain styled]
          body  '[static static-styled]]
    (println :! main body)
    (render-visual-compare!
     :template  (format "multipart-gen/main-%s.docx" main)
     :data      {:name "Janos"}
     :expected  (format "multipart-gen/expected-%s-%s.pdf" main body) :fix? true
     :fragments {"body"   (format "multipart-gen/body-%s.docx" body)
                 ;"header" "multipart/header.docx"
                 ;"footer" "multipart/footer.docx"
                 })))
