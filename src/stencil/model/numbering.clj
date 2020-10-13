(ns stencil.model.numbering
  (:import [java.io File]
           [java.nio.file Files]
           [io.github.erdos.stencil.impl FileHelper])
  (:require [clojure.data.xml :as xml]
            [clojure.data.xml.pu-map :as pu]
            [clojure.java.io :as io :refer [file]]
            [clojure.walk :refer [postwalk]]
            [stencil.ooxml :as ooxml]
            [stencil.eval :as eval]
            [stencil.merger :as merger]
            [stencil.tree-postprocess :as tree-postprocess]
            [stencil.types :refer [->FragmentInvoke]]
            [stencil.util :refer :all]
            [stencil.cleanup :as cleanup]))

(defn parse
  [numbering-file]
  (assert numbering-file)
  (with-open [r (io/input-stream (file numbering-file))]
    (into {} []))
  )


(defn render [model]
  nil
  )
