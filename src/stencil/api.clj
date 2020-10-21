(ns stencil.api
  "A simple public API for document generation from templates."
  (:require [clojure.walk :refer [stringify-keys]]
            [clojure.java.io :as io]
            [clojure.pprint :refer [simple-dispatch]]
            [stencil.types])
  (:import [io.github.erdos.stencil API PreparedFragment PreparedTemplate TemplateData]
           [stencil.types OpenTag CloseTag TextTag]
           [java.io InputStreamReader]
           [java.util Map]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defmethod simple-dispatch OpenTag [t] (print (str "<" (:open t) ">")))
(defmethod simple-dispatch CloseTag [t] (print (str "</" (:close t) ">")))
(defmethod simple-dispatch TextTag [t] (print (str "'" (:text t) "'")))

(defn ^PreparedTemplate prepare
  "Creates a prepared template instance from an input document."
  [input]
  (cond
    (instance? PreparedTemplate input) input
    (nil? input)  (throw (ex-info "Template is missing!" {}))
    :else         (API/prepare (io/file input))))


(defn- ^TemplateData make-template-data [x]
  (if (map? x)
    (TemplateData/fromMap ^Map (stringify-keys x))
    (throw (ex-info (str "Unsupported template data type " (type x) "!")
                    {:template-data x}))))


(defn ^PreparedFragment fragment
  "Converts input to a fragment instance"
  [f]
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
      (.writeToStream result (:output opts))

      (:output opts)
      (let [f (io/file (:output opts))]
        (when (.exists f)
          (if (:overwrite? opts)
            (.delete f)
            (throw (ex-info "File already exists! " {:file f}))))
        (do (.writeToFile result f)
            (str "Written to " f)))

      :else
      result)))


(defn cleanup! [template]
  (cond (instance? PreparedTemplate template) (.cleanup ^PreparedTemplate template)
        (instance? PreparedFragment template) (.cleanup ^PreparedFragment template)
        :else (throw (ex-info "Unexpected object to clean up!" {:template template})))
  template)

(defmacro ^:private get-version [] (System/getProperty "stencil-core.version"))

(def version (get-version))
