(ns stencil.api
  "A simple public API for document generation from templates."
  (:require [clojure.java.io :as io]
            [clojure.walk :refer [stringify-keys]])
  (:import [io.github.erdos.stencil API PreparedTemplate TemplateData]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn prepare
  "Creates a prepared template instance from an input document."
  [input]
  (cond
    (instance? PreparedTemplate input) input
    (nil? input)  (throw (ex-info "Template is missing!" {}))
    :otherwise    (API/prepare (io/file input))))

(defn- make-template-data [x]
  (if (map? x)
    (TemplateData/fromMap ^java.util.Map (stringify-keys x))
    (throw (ex-info (str "Unsupported template data type " (type x) "!")
                    {:template-data x}))))

(defn render!
  "Takes a prepared template instance and renders it.
   By default it returns an InputStream of the rendered document.

  Options map keys:
  - {:output FNAME} renders output to file FNAME (string or File object). Throws exception
    if file already exists and :overwrite? option is not set.
  - {:output :input-stream} returns an input stream of the result document.
  - {:output :reader} returns the input stream reader of the result document."
  [template template-data & {:as opts}]
  (let [template      (prepare template)
        template-data (make-template-data template-data)
        result (API/render template ^TemplateData template-data)]
    (cond
      (#{:stream :input-stream} (:output opts))
      (.getInputStream result)

      (#{:reader} (:output opts))
      (new java.io.InputStreamReader (.getInputStream result))

      (:output opts)
      (let [f (io/file (:output opts))]
        (when (.exists f)
          (if (:overwrite? opts)
            (.delete f)
            (throw (ex-info "File already exists! " {:file f}))))
        (do (.writeToFile result f)
            (str "Written to " f)))

      :otherwise
      result)))
