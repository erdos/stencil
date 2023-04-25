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
  (let [resolution    144
        basename      (str (java.util.UUID/randomUUID))
        outdir        (io/file (or (System/getenv "RUNNER_TEMP") "/tmp") "stencil-testing")
        docx-output   (io/file outdir (str basename ".docx"))
        pdf-output    (io/file outdir (str basename ".pdf"))
        png-output    (io/file outdir (str basename ".png"))
        expected-img  (io/file (io/resource expected-img-file))
        expected-png  (some-> expected-img (.getName) (.replaceFirst "\\.[a-z]{3,4}$" ".png") (->> (io/file outdir)))]
    (io/make-parents docx-output)

    ;; 1. render template
    (with-open [template (api/prepare (io/resource template-name))]
      (api/render! template data
                   :output docx-output
                   :overwrite? true
                   :fragments (into {} (for [[k v] fragments] [k (api/fragment (io/resource v))]))))

    ;; 2. convert rendered docx to PDF
    (let [converted (shell/sh "libreoffice" "--headless" "--convert-to" "pdf" "--outdir" (str outdir) (str docx-output))]
      (is (= 0 (:exit converted)) (str "PDF Error: " (pr-str converted)))
      (is (.exists pdf-output) (str "Output PDF file does not exist: " pdf-output)))

    ;; 3. convert expected PDF to png
    (assert (some? expected-img)
            (format "Expected file %s does not exist, cannot compare to %s" expected-img pdf-output))
    (let [conversion (shell/sh "convert" "-density" (str resolution) (str expected-img) "-background" "white" "-alpha" "remove" "-blur" "6x6" (str expected-png))]
      (is (= 0 (:exit conversion))
          (format "Conversion error: %s" (pr-str conversion)))
      (is (.exists expected-png)))

    ;; 4. convert PDF to png
    (let [conversion (shell/sh "convert" "-density" (str resolution) (str pdf-output) "-background" "white" "-alpha" "remove" "-blur" "6x6" (str png-output))]
      (is (= 0 (:exit conversion)) (str "Conversion error: " (pr-str conversion)))
      (is (.exists png-output)))

    ;; 5. visually compare png to expected
    (let [diff-output   (io/file outdir (str basename ".diff.png"))
          compared      (shell/sh "compare" "-verbose" "-metric" "AE" "-fuzz" "2%"
                                  (str expected-png)
                                  (str png-output)
                                  (str diff-output))]
      (is (= 0 (:exit compared)) (str "Error comparing" (pr-str compared))))))

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