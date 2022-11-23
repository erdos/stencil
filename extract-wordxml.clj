#!/usr/bin/env bb

(def input (first *command-line-args*))
(assert input)

(def input-file (clojure.java.io/file input))
(assert (.exists input-file))

(with-open [fistream  (new java.io.FileInputStream input-file)
            zipstream (new java.util.zip.ZipInputStream fistream)]
  (doseq [entry (repeatedly #(.getNextEntry zipstream))
          :while entry
          :when (= "word/document.xml" (.getName entry))]
    (-> zipstream
        (clojure.data.xml/parse)
        (clojure.data.xml/indent-str)
        (println))))
