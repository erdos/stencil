(ns stencil.model.content-types
  (:require [clojure.java.io :refer [file]])
  (:import [java.io File]))

(defn parse-content-types [^File dir]
  (assert (.isDirectory dir))
  (let [cts (file dir "[Content_Types].xml")]
    (assert (.exists cts))
    (assert (.isFile cts))
    {:source-file cts
     :stencil.model/path       (.getName cts)}))
