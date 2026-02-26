(ns stencil.api-test
  (:import [io.github.erdos.stencil.exceptions EvalException]
           [java.nio.file Paths])
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.java.io :as io]
            [stencil.api :refer [prepare render! fragment cleanup!]]
            [stencil.functions :refer [call-fn]]))

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
        (with-open [out (io/output-stream f)]
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

(deftest test-prepare+fragment-types
  (let [fname "./examples/Multipart Template/footer.docx"]
    (doseq [testable [prepare, fragment]
            data [fname, (io/file fname), (Paths/get fname (make-array String 0))]]
      (is (some? (testable data))))))

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

(deftest test-image
  (let [data {"image" "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIYAAACnCAYAAADUiSQCAAAGhklEQVR4Xu3d0ZbTMAxFUebP+fOyWihkStpIlmTJ1uGR5SS2tHvthgG+breftx/8ogIvFfgCBibOKgAMXJxWABjAAAYG5BUgMeS1ajUSGK3aLV8sMOS1ajUSGK3aLV8sMOS1ajUSGK3aLV8sMOS1ajUSGK3aLV8sMOS1ajUSGK3aLV8sMOS1ajUSGK3aLV8sMOS1ajUSGK3aLV8sMOS1ajUSGK3aLV8sMOS1ajUSGK3aLV8sMOS1ajUSGK3aLV8sMOS1Oh359ed3d/tbW8AwwHiiuN8CGIZCSi69F3uFIh9RAEPSWeOYFaL5FQUwjE2XXF49ns9QPNe1QtJJenAfU+6MUTmiP6HYLTWAIfwIXaEAhrCQlmEVU0MCYycc5RLjsb+dqMrcv6UogGGJA8G17xqRgUODAhiC5lqGVDn5a1EAw9J1wbVXDZmRHFdz+LSMGfMTlNE0pOQZ490547jSyOJbUOySGsvCiGqAFUXUvEwf/4GLgXEomgcKYAwo1FwibZLXliJ9nnQNXvOSPs973NKJ8SyGtQneKHZIjbIwJAdQj8NoBApgeOfXy/20TdMmh/b+2uVq56O9f+T4bRJDu61Eo1g9NbaDIW0IMD7nzZYwrnDMQHE1h8htwOPepWFoD6CSw+gsFNqtzaOZnvfYFsbZJ3Y2ipVTY2sYx8ZkoACGZ4YZv7IGTmX41it+bd0+MYa76XghMByLebxV1jbgtRxgeFVys+0EGMB4W4HVcJQ/Y1jeZQQ5HbotMIbKdn0R54zrGnmOWCIxdkgNEsOT7eFeqyfGai+7SIwgyGe3XSk1gAGM0wosA2OHc8ZK2wkwJiYGMIKKzQE0qLAntyUx5tX68aRVDqDAmAzj+bjqQJaCscsB9GixKhA3GM/9P3qhO5wzVnjH4QrjjiIayK4wqm0xbjDexbx3guwOowoQVxifzgBeQLrAyAYyDYbnQrvhyPia6w5D+s3BkiAdYXh+sCTf0NNgWBbaGYalbhIQzzEhMKSp8TpRaYoA41/lpDXToHj073b7GXXv03/hVzLBqwkB4/8qXtVMUvfjmJIwJHEJjvNWewEJhTG6pUi2GGB8zgArkCVgnCUIMGSbwyiQcBheqXEsw/HVu6w8fUe1gtG3zbqVj6II/1by7ZSrWxOjjRWwoJgKI2JLMdZuy8utIMJfcJ1VnQNjrEUvFNMTg9SIg+GJAhhxfZp6Z28UKTBIDT8zESBSzhh8S1kDRVpikBo2IJFJkZ4Y4BjDMQNFamIAQwdjFogSiQEOGY7ZKNIT469OWX1ajspAAYzi1LJQlIHBlvJdaCaIMmcMtpN6KEolBqnxG0iFtABG0TNGBRxTfrRPU3/+aL5GapSDwZZSY0sBhibOJo/N3FJKwiA1/gnMwgGMySkw8rgMHGVhkBq5qVEaBjjycABjJNuTrpm5pQAjqcmjj52FozQMXnb9z6c9DFC8z5QZOEomBiiuN5poHOVggOIaxXNEJA5gyPtQcmQUjlIwSAu9ve1hgEKPInJLKZEYoBhHEYUjHQYo7CgicKTCAIUfCm8caTBA4Y/ifkevw2gKDFDEoPBMjekwQBGLwgsHMOb0KeUplm1lKgzSYq6PJWB0QPGuEZlrH8UxJTEyCzPzM6ptwqy6aOf1+JHKyP/I5vGAmZ1JfNZI8d9NN6Jm2vmFwohYYGLvPz5aW3jtOjxqqZljGAyPhWiLlzVeU3DvOWrqrJlnCAzNZL0LlXE/TcFnze9dD6RzdYfRDYXna+hZaCTPWRrGJ/2zgEo/gZJmVBrjCsO7Gdaie8/nrHHWOVbCcJyLG4zRJkQWdnROmmZFzl8zD++xLjA+NSC7cJE4stfmjSEkMSInab13FA5gWDuTfH0EjJ1RTHklnmzi7+O9cQCjSmeN8wCGroAuh0/dI/NGe+HYPS1abSWPxTqZBIZTISvdxgMHMCp11HEuFhwdULTbSp62gHH9KWt1+Pz2Zu+6NqcjSIzBwq1y2UhqdEHRdisZ3VKAscrH3jhPTWp0QtE+MTTvNoBh/BSueLkkOYCxYmeNc76C0Q0FW8kBVOUfNjK6H7q87XuM12oB43tFgHGRGh23EbaSl9g4Sw1gDO1E+110xNEVBYnxxvUTBzD2++CbVgSMCf8+hqlDXJxWAb6VpJW+9oOBUbs/abMDRlrpaz8YGLX7kzY7YKSVvvaDgVG7P2mzA0Za6Ws/GBi1+5M2O2Cklb72g4FRuz9ps/sFPAT5UQuOEXMAAAAASUVORK5CYII="}
        f    (java.io.File/createTempFile "stencil" ".docx")]
    (with-open [template (prepare "test-resources/test-image-1.docx")]
      (render! template data :output f :overwrite? true))
    (is true)))

(deftest test-link
  (let [data {"url" "https://stencil.erdos.dev/?data=1&data2=2"}
        f    (java.io.File/createTempFile "stencil" ".docx")]
    (with-open [template (prepare "test-resources/test-link-1.docx")]
      (render! template data :output f :overwrite? true))
    (is true)))

(deftest test-multipart
  (let [template (prepare "test-resources/multipart/main.docx")
        body     (fragment "test-resources/multipart/body.docx")
        header   (fragment "test-resources/multipart/header.docx")
        footer   (fragment "test-resources/multipart/footer.docx")
        data     {:name "John Doe"}]
    ;; ~51ms on the author's machine
    (render! template data
             :fragments {"body"   body
                         "header" header
                         "footer" footer}
             :output "/tmp/out-multipart.docx"
             :overwrite? true)
    (is true)))

(deftest test-custom-function
  (with-open [template (prepare "test-resources/test-custom-function.docx")]
    (let [called (atom false)
          f (java.io.File/createTempFile "stencil" ".docx")]
      (try
        (defmethod call-fn "customFunction"
          [_ & args]
          (reset! called true)
          (first args))
        (render! template {:input "data"} :output f :overwrite? true)
        (is @called)
        (finally (remove-method call-fn "customFunction"))))))
