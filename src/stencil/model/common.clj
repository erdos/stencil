(ns stencil.model.common
  (:import [java.nio.file Files]
           [java.io File]
           [io.github.erdos.stencil.impl FileHelper])
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]))


(defn ->xml-writer [tree]
  (fn [output-stream]
    (let [writer (io/writer output-stream)]
      (xml/emit tree writer)
      (.flush writer))))


(defn resource-copier [x]
  (assert (:stencil.model/path x))
  (assert (:source-file x))
  (fn [writer]
    (let [stream (io/output-stream writer)]
      (Files/copy (.toPath (io/file (:source-file x))) stream)
      (.flush stream)
      nil)))


(defn unix-path [^File f]
  (some-> f .toPath FileHelper/toUnixSeparatedString))
