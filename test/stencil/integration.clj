(ns stencil.integration
  "Integration test helpers"
  (:require [clojure (zip :as zip) (test :refer [do-report is])]
            [clojure.data.xml]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [stencil (api :as api) (util) (ooxml)]))


(defn rendered-words
  "Renders a template with a given data, returns the list of text runs from the result."
  [template-name data]
  (let [template-file (clojure.java.io/resource template-name)]
    (assert template-file "Missing template file!")
    (with-open [template (api/prepare template-file)
                stream   (api/render! template data :output :stream)
                zipstream (new java.util.zip.ZipInputStream stream)]
      (doall
       (for [entry (repeatedly (fn [] (.getNextEntry zipstream)))
             :while entry
             :when (= "word/document.xml" (.getName entry))
             :let [tree (stencil.util/xml-zip (clojure.data.xml/parse zipstream))]
             node (iterate clojure.zip/next tree)
             :while (not (clojure.zip/end? node))
             :when (map? (zip/node node))
             :when (= stencil.ooxml/t (:tag (zip/node node)))
             c (:content (zip/node node))]
         c)))))

(defn render-visual-compare!
  "Render a file and then visually compare result to a screenshot"
  [& {template-name :template
      data :data
      expected-img-file :expected
      fragments :fragments}]
  (let [docx-output   (java.io.File/createTempFile "stencil-test" ".docx")
        outdir        (doto (io/file (or (System/getenv "RUNNER_TEMP") "/tmp") "stencil-testing")
                        io/make-parents)
        resolution    72
        pdf-output    (io/file outdir (.replaceFirst (.getName docx-output) ".docx$" ".pdf"))
        png-output    (io/file outdir (str (rand-int 10000) ".png"))]
    ;; 1. render template
    (with-open [template (api/prepare (io/resource template-name))]
      (api/render! template data
                   :output docx-output
                   :overwrite? true
                   :fragments (into {} (for [[k v] fragments] [k (api/fragment (io/resource v))])))
      (println "Renderd file to" docx-output))
    ;; 2. convert rendered docx to PDF
    (let [converted (shell/sh "libreoffice" "--headless" "--convert-to" "pdf" "--outdir" (str outdir) (str docx-output))]
      (assert (= 0 (:exit converted)) (str "PDF Error: " (pr-str converted)))
      (assert (.exists pdf-output) (str "Output PDF file does not exist: " pdf-output)))

    ;; 3. convert PDF to png
    (let [conversion (shell/sh "convert" "-density" (str resolution) (str pdf-output) "-background" "white" "-alpha" "remove" (str png-output))]
      (assert (= 0 (:exit conversion)) (str "Conversion error: " (pr-str conversion)))
      (assert (.exists png-output)))

    ;; 4. visually compare png to expected
    (if-not expected-img-file
      (do (println "Rendered file to" png-output)
          (is false "test mode only"))
      (let [diff-output   (io/file (str png-output ".diff.png"))
            compared      (shell/sh "compare" "-verbose"
                                    (str (io/file (io/resource expected-img-file)))
                                    (str png-output)
                                    (str diff-output))]
        (is (= 0 (:exit compared)) (str "Error comparing" (pr-str compared)))))))

(defn test-fails
  "Tests that rendering the template with the payload results in the given exception chain."
  [template payload & bodies]
  (assert (string? template))
  (assert (not-empty bodies))
  (try (with-open [template (api/prepare template)]
         (api/render! template payload
                      :overwrite? true
                      :output (java.io.File/createTempFile "stencil" ".docx")))
       (do-report {:type :error
                   :message "Should have thrown exception"
                   :expected (first bodies)
                   :actual nil})
       (catch RuntimeException e
         (let [e (reduce (fn [e [t reason]]
                           (is (instance? t e))
                           (or (= t NullPointerException) (is (= reason (.getMessage e))))
                           (.getCause e))
                         e (partition 2 bodies))]
           (is (= nil e) "Cause must be null.")))))