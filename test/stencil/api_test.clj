(ns stencil.api-test
  (:import [io.github.erdos.stencil.exceptions EvalException])
  (:require [clojure.test :refer [deftest testing is are]]
            [stencil.api :refer :all]))

(deftest test-prepare+render+cleanup
  (let [template (prepare "./examples/Purchase Reminder/template.docx")
        data {:customerName "John Doe",
              :shopName "Example Shop",
              :items [{:name "Dog food",
                         :description "Tastes good.",
                         :price "$ 123"},
                        {:name "Duct tape",
                         :description "To fix things.",
                         :price "$ 12"},
                        {:name "WD-40",
                         :description "To fix other things.",
                         :price "$ 12"}],
              :total "$ 147"}]
    (testing "template data can not be produced"
      (is (thrown? clojure.lang.ExceptionInfo (render! template "{}"))))
    (testing "Rendering without writing file"
      (render! template data))
    (testing "Writing output file"
      (let [f (java.io.File/createTempFile "stencil" ".docx")]
        (.delete f)
        (render! template data :output f)
        (is (.exists f))))

    (testing "Writing to existing file fails"
      (let [f (java.io.File/createTempFile "stencil" ".docx")]
        (is (thrown? clojure.lang.ExceptionInfo
                     (render! template data :output f)))))

    (testing "Writing to existing file with overwrite flag enabled"
      (let [f (java.io.File/createTempFile "stencil" ".docx")]
        (render! template data :output f :overwrite? true)
        (is (.exists f))))

    (testing "Can render output to output-stream"
      (let [f (java.io.File/createTempFile "stencil" ".docx")]
        (is (= 0 (.length f)))
        (with-open [out (clojure.java.io/output-stream f)]
          (render! template data :output out))
        (is (not= 0 (.length f)))))

    (testing "Can create an input stream"
      (let [f (java.io.File/createTempFile "stencil" ".docx")]
        (.delete f)
        (let [istream (render! template data :output :input-stream)]
          (java.nio.file.Files/copy istream (.toPath f) (into-array java.nio.file.CopyOption [])))
        (is (not= 0 (.length f)))))

    (testing "Cannot render in a transaction"
      (let [f (java.io.File/createTempFile "stencil" ".docx")]
        (dosync
         (is (thrown? IllegalStateException (render! template data :output f :overwrite? true))))))

    (testing "Preparing twice returns same object"
      (is (identical? template (prepare template))))

    (testing "After cleanup"
      (cleanup! template)
      (testing "Subsequent cleanup call has no effect"
        (cleanup! template))
      (testing "Rendering fails on cleaned template"
        (is (thrown? IllegalStateException (render! template data)))))))

(deftest test-prepare-nil
  (is (thrown? clojure.lang.ExceptionInfo (prepare nil))))

(deftest test-fragment-nil
  (is (thrown? clojure.lang.ExceptionInfo (fragment nil))))

(deftest test-fragment
  (let [f (fragment "./examples/Multipart Template/footer.docx")]
    (is (some? f))
    (is (identical? f (fragment f)))
    (cleanup! f)
    (testing "Subsequent invocations have no effect"
      (cleanup! f))))

(deftest test-render-with-fragments
  (let [footer (fragment "./examples/Multipart Template/footer.docx")
        header (fragment "./examples/Multipart Template/header.docx")
        static (fragment "./examples/Multipart Template/static.docx")
        template (prepare "./examples/Multipart Template/template.docx")
        data {:companyName "ACME" :companyAddress "Moon"}
        fs-map {:footer footer :header header :static static}]
    (testing "Rendering multipart template"
      (render! template data :fragments fs-map))
    (testing "Can not render when fragments can not be found."
      (is (thrown? EvalException (render! template data :fragments {}))))
    (testing "Can not render when a fragment is cleared"
      (cleanup! header)
      (is (thrown? IllegalStateException (render! template data :fragments fs-map))))))

(deftest test-cleanup-fails
  (is (thrown? clojure.lang.ExceptionInfo (cleanup! nil)))
  (is (thrown? clojure.lang.ExceptionInfo (cleanup! "asdasdad"))))

(comment ;; try to prepare then render a DOCX file

  (def template-1 (prepare "/home/erdos/Joy/stencil/test-resources/test-control-conditionals.docx"))

  (defn render-template-1 [output-file data]
    (render! template-1 data :output output-file))

  (render-template-1 "/tmp/output-3.docx" {"customerName" "John Doe"}))

(comment ;; try to prepare then render a PPTX presentation file

  (def template-2 (prepare "/home/erdos/example-presentation.pptx"))

  (defn render-template-2 [output-file data]
    (render! template-2 data :output output-file))

  (render-template-2 "/tmp/output-7.pptx"
                     {"customerName" "John Doe" "x" "XXX" "y" "yyyy"}))

(deftest test-prepare-fragment-names
  (with-open [template (prepare "test-resources/multipart/main.docx")]
    (is (= #{"header" "footer" "body"} (-> template .getVariables .getAllFragmentNames)))))

(deftest test-prepare-variable-names
  (with-open [template (prepare "test-resources/multipart/header.docx")]
    (is (= #{"name"} (-> template .getVariables .getAllVariables)))))

(comment

  (let [template (prepare "test-resources/multipart/main.docx")
        body     (fragment "test-resources/multipart/body.docx")
        header   (fragment "test-resources/multipart/header.docx")
        footer   (fragment "test-resources/multipart/footer.docx")
        data     {:name "John Doe"}]
    ;; ~51ms on the author's machine
    (time
     (render! template data
              :fragments {"body"   body
                          "header" header
                          "footer" footer}
              :output "/tmp/out1.docx"
              :overwrite? true)))


  )
