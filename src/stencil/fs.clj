(ns stencil.fs
  (:import [java.io File]
           [io.github.erdos.stencil.impl FileHelper]))

(set! *warn-on-reflection* true)

(defn exists? [^File file]
  (.exists file))

(defn directory? [^File file]
  (.isDirectory file))

(defn unix-path ^String [^File f]
  (some-> f .toPath FileHelper/toUnixSeparatedString))

(defn parent-file ^File [^File f]
  (.getParentFile f))

;; remove /../ parts
(defn unroll [^File f] (-> f .toPath .normalize .toFile))
