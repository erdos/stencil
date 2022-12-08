(ns stencil.integration
  "Integration test helpers"
  (:require [clojure.zip :as zip]
            [clojure.test :refer [do-report is]]
            [stencil.api :as api]))


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