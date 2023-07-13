(ns stencil.visual
  (:require [clojure.java.shell]
            [clojure.string :as str]
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
  (doseq [main '[plain styled plain-with-header styled-with-header]
          body  '[static static-styled]
          header '[nil static plain styled]
          :when (= (str/includes? (name main) "with-header") (some? header))]
    (render-visual-compare!
     :template  (format "multipart-gen/main-%s.docx" main)
     :data      {:name "Janos"}
     :expected  (format "multipart-gen/expected-%s-%s-%s.pdf" main body header)
     :fix? true
     :fragments {"body"   (format "multipart-gen/body-%s.docx" body)
                 "header" (when header (format "multipart-gen/header-%s.docx" header))})))

