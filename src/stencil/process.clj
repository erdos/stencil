(ns stencil.process
  "These functions are called from Java."
  (:gen-class)
  (:import [java.io File PipedInputStream PipedOutputStream InputStream]
           [java.util.zip ZipEntry ZipOutputStream]
           [io.github.erdos.stencil.impl FileHelper ZipHelper])
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [stencil.postprocess.ignored-tag :as ignored-tag]
            [stencil
             [util :refer :all]
             [model :as model]
             [tokenizer :as tokenizer]
             [cleanup :as cleanup]
             [eval :as eval]
             [tree-postprocess :as tree-postprocess]]))

(set! *warn-on-reflection* true)

(defn prepare-template [^InputStream stream]
  (assert (instance? InputStream stream))
  (let [zip-dir   (FileHelper/createNonexistentTempFile "stencil-" ".zip.contents")]
    (with-open [zip-stream stream] ;; FIXME: maybe not deleted immediately
      (ZipHelper/unzipStreamIntoDirectory zip-stream zip-dir))
    (model/load-template-model zip-dir)))

(defn prepare-fragment [input]
  (assert (some? input))
  (let [zip-dir (FileHelper/createNonexistentTempFile
                 "stencil-fragment-" ".zip.contents")]
    (with-open [zip-stream (io/input-stream input)]
      (ZipHelper/unzipStreamIntoDirectory zip-stream zip-dir))
    (model/load-fragment-model zip-dir)))

(defn eval-template [{:keys [template data function fragments] :as args}]
  (assert (:source-folder template))
  (let [data   (into {} data)

        evaled-template-model (model/eval-template-model template data function fragments)
        writers-map (model/evaled-template-model->writers-map evaled-template-model)

        outstream    (new PipedOutputStream)
        input-stream (new PipedInputStream outstream)]
    (future
      (try
        (with-open [zipstream (new ZipOutputStream outstream)]
          (doseq [[k writer] writers-map
                  :let  [rel-path (FileHelper/toUnixSeparatedString (.toPath (io/file k)))
                         ze       (new ZipEntry rel-path)]]
            (.putNextEntry zipstream ze)
            (writer zipstream)
            (.closeEntry zipstream)))
        (catch Throwable e
          (println "Zipping exception: " e))))
    {:stream input-stream}))
