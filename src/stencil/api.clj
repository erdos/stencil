(ns stencil.api
  "A simple public API for document generation from templates."
  (:import [io.github.erdos.stencil API PreparedFragment PreparedTemplate TemplateData]
           [java.io InputStreamReader]
           [java.util Map])
  (:require [clojure.walk :refer [stringify-keys]]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn ^PreparedTemplate prepare
  "Creates a prepared template instance from an input document."
  [input]
  (cond
    (instance? PreparedTemplate input) input
    (nil? input)  (throw (ex-info "Template is missing!" {}))
    :otherwise    (API/prepare (io/file input))))

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
    :otherwise (API/fragment (io/file f))))

(comment
  (time (fragment "/home/erdos/Work/moby-aegon/templates/stencil/DIJELSZAMOLAS.docx"))


  (render! "/home/erdos/stencil/test-resources/multipart/main.docx"
           {:name "John Doe"}
           :overwrite? true
           :output "/home/erdos/stencil-fragments-out.docx"
           :fragments {"header" "/home/erdos/stencil/test-resources/multipart/header.docx"
                       "footer" "/home/erdos/stencil/test-resources/multipart/footer.docx"
                       "body" "/home/erdos/stencil/test-resources/multipart/body.docx"})



  comment)


(defn render!
  "Takes a prepared template instance and renders it.
   By default it returns an InputStream of the rendered document.

  Options map keys:
  - {:fragments {\"fragmentName\" fragment-object}} map of document fragments to embed in template.
  - {:output FNAME} renders output to file FNAME (string or File object). Throws exception
    if file already exists and :overwrite? option is not set.
  - {:output :input-stream} returns an input stream of the result document.
  - {:output :reader} returns the input stream reader of the result document."
  [template template-data & {:as opts}]
  (let [template      (prepare template)
        fragments     (into {} (for [[k v] (:fragments opts)] [(name k) (fragment v)]))
        template-data (make-template-data template-data)
        result (API/render template fragments template-data)]
    (cond
      (#{:stream :input-stream} (:output opts))
      (.getInputStream result)

      (#{:reader} (:output opts))
      (new InputStreamReader (.getInputStream result))

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


(defn cleanup! [template]
  (assert (instance? PreparedTemplate template))
  (doto ^PreparedTemplate template
    (.cleanup)))
