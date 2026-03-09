(ns stencil.fs
  (:import [java.io File]
           [java.nio.file Path]
           [io.github.erdos.stencil.impl FileHelper]))

(set! *warn-on-reflection* true)

(defn exists? [^File file]
  (.exists file))

(defn directory? [^File file]
  (.isDirectory file))

(defn unix-path
  "Returns a string representation of path with unix separators ('/')
   instead of the system-dependent separators (which is backslash on Windows)."  
  ^String [^File f]
  (when f
    (let [path (.toPath f)]
      (.replace (str path) (.getSeparator (.getFileSystem path)) "/"))))

(defn parent-file ^File [^File f]
  (.getParentFile f))

;; remove /../ parts
(defn unroll [^File f] (-> f .toPath .normalize .toFile))

(defn delete!
  "Recursively delete a directory or a single file."
  [^File f]
  (FileHelper/forceDelete f))

(defn ->tmp-file
  "Create a new temporary file in the parent directory."
  [^File parent prefix suffix]
  (FileHelper/createNonexistentTempFile parent prefix suffix))

(defn path->input-stream ^java.io.InputStream [^Path path]
  (java.nio.file.Files/newInputStream path (into-array java.nio.file.OpenOption [])))
