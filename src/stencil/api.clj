(ns stencil.api
  "A simple public API for document generation from templates."
  (:require [clojure.walk :refer [stringify-keys]]
            [clojure.java.io :as io])
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


(defn cleanup! [template]
  (assert (instance? PreparedTemplate template))
  (doto template
    (.cleanup)))

(comment

  (def prepared (prepare "/home/erdos/EP_adatbekero-sablon.docx"))

  (render! prepared
           {:nev "DBX Kft."

            :levirszam "1027"
            :levhelyseg "Budapest"
            :levutca "Bem rakpart 56"
            :ugyintezo "Teszt Elek"
            :iktatoszam "IKTATOSZAM001"
            :datum "2018.01.30"
            :adoszam "00010001001"
            :email "info@dbx.hu"

            :hibak [{:nev "Első Hiba" :teendo "Első Teendő"}
                    {:nev "Második Hiba" :teendo "Második Teendő"}
                    {:nev "Második Hiba" :teendo "Második Teendő"}]}
           :overwrite? true
           :output "/home/erdos/EP_adatbekero-kimenet.docx")

  )
