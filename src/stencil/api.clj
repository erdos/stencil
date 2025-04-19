(ns stencil.api
  "A simple public API for document generation from templates."
  (:require [clojure.walk :refer [stringify-keys]]
            [clojure.java.io :as io]
            [stencil.fs :as fs])
  (:import [io.github.erdos.stencil API PreparedFragment PreparedTemplate TemplateData]
           [java.util Map]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn prepare
  "Creates a prepared template instance from an input document."
  ^PreparedTemplate [input]
  (cond
    (instance? PreparedTemplate input) input
    (nil? input)  (throw (ex-info "Template is missing!" {}))
    :else         (API/prepare (io/file input))))


(defn- make-template-data ^TemplateData [x]
  (if (map? x)
    (TemplateData/fromMap ^Map (stringify-keys x))
    (throw (ex-info (str "Unsupported template data type " (type x) "!")
                    {:template-data x}))))


(defn fragment
  "Converts input to a fragment instance"
  ^PreparedFragment [f]
  (cond
    (instance? PreparedFragment f) f
    (nil? f)   (throw (ex-info "Fragment can not be null!" {}))
    :else      (API/fragment (io/file f))))


(defn render!
  "Takes a prepared template instance and renders it.
   By default it returns an InputStream of the rendered document.

  Options map keys:
  - {:fragments {\"fragmentName\" fragment-object}} map of document fragments to embed in template.
  - {:output FNAME} renders output to file FNAME (string or File object). Throws exception
    if file already exists and :overwrite? option is not set.
  - {:output STREAM} writes output to an OutputStream object.
  - {:output :input-stream} returns an input stream of the result document."
  [template template-data & {:as opts}]
  (let [template      (prepare template)
        fragments     (into {} (for [[k v] (:fragments opts)] [(name k) (fragment v)]))
        template-data (make-template-data template-data)
        result (API/render template fragments template-data)]
    (cond
      (#{:stream :input-stream} (:output opts))
      (.toInputStream result clojure.lang.Agent/soloExecutor)

      (instance? java.io.OutputStream (:output opts))
      (.write result (:output opts))

      (:output opts)
      (let [f (io/file (:output opts))]
        (when (fs/exists? f)
          (if (:overwrite? opts)
            (.delete f)
            (throw (ex-info "File already exists! " {:file f}))))
        (do (.writeToFile result f)
            (str "Written to " f)))

      :else
      result)))


(defn cleanup! [template]
  (cond (instance? PreparedTemplate template) (.close ^PreparedTemplate template)
        (instance? PreparedFragment template) (.close ^PreparedFragment template)
        :else (throw (ex-info "Unexpected object to clean up!" {:template template})))
  template)

(defmacro get-version ^:private [] (slurp (io/resource "stencil-version")))
(def version (doto (get-version) (assert)))
