#!/usr/bin/env bb

(def input (first *command-line-args*))
(assert input)

(def input-file (clojure.java.io/file input))
(assert (.exists input-file))

(println (str input-file))
(println)

(with-open [fistream  (new java.io.FileInputStream input-file)
            zipstream (new java.util.zip.ZipInputStream fistream)]
  (->> (for [entry (repeatedly #(.getNextEntry zipstream))
             :while entry
             :when (or (.endsWith (.getName entry) ".xml")
                       (.endsWith (.getName entry) ".xml.rels"))]
         (vector (.getName entry)
                 (-> zipstream
                     (clojure.data.xml/parse)
                     (clojure.data.xml/indent-str))))
       (sort-by first)
       (run! (fn [[k v]] (println k) (println) (println v)))))