(ns stencil.process
  "These functions are called from Java."
  (:gen-class)
  (:import [java.io File PipedInputStream PipedOutputStream InputStream]
           [java.util.zip ZipEntry ZipOutputStream]
           [io.github.erdos.stencil.impl FileHelper ZipHelper])
  (:require [clojure.java.io :as io]
            [stencil.util :refer [trace]]
            [stencil.model :as model]))

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

(defn- render-writers-map [writers-map outstream]
  (assert (map? writers-map))
  (with-open [zipstream (new ZipOutputStream outstream)]
    (doseq [[k writer] writers-map
            :let  [rel-path (FileHelper/toUnixSeparatedString (.toPath (io/file k)))
                   ze       (new ZipEntry rel-path)]]
      (assert (not (.contains rel-path "../")))
      (trace "ZIP: writing %s" rel-path)
      (.putNextEntry zipstream ze)
      (writer zipstream)
      (.closeEntry zipstream))))

(defn eval-template [{:keys [template data function fragments] :as args}]
  (assert (:source-folder template))
  (let [data        (into {} data)
        writers-map (model/template-model->writers-map template data function fragments)]
    {:writer (partial render-writers-map writers-map)}))
