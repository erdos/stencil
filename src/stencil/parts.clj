(ns stencil.parts
  "This ns is used to addeg generated content as parts of DOCX files."
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [stencil.util :refer :all]))


(def ^:dynamic *parts-data* nil)

(defmacro with-parts-data [body]
  `(binding [*parts-data* (atom [])] ~body))

(defn register-external! [& {:as opts}]
  (assert (:id opts))
  (assert (:file-name opts))
  (swap! *parts-data* conj opts))

;; returns a function that writes its content given an output-stream
(defn render-relations [rels-file]
  (assert (instance? java.io.File rels-file))
  (->
   (with-open [reader (clojure.java.io/reader rels-file)]
     (xml/parse reader))
   (update
    :content concat
    (for [data @*parts-data*]
      {:tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationship
       :attrs {:Id     (str (:id data))
               :Target (str (:file-name data)) ;; ide altalaban nem kell slash
               :Type   (str (:rel-type data))
               :TargetMode (:rel-target-mode data)}}))
   (as-> data (fn [output-stream]
                (let [writer (io/writer output-stream)]
                  (xml/emit data writer)
                  (.flush writer))))))

;; returns a function that writes its content given an output-stream
(defn render-content-types [content-types-file]
  (assert (instance? java.io.File content-types-file))
  (->
   (with-open [reader (clojure.java.io/reader content-types-file)]
     (xml/parse reader))
   (update
    :content concat
    (for [data @*parts-data*]
      {:tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Fcontent-types/Override
       ;; TODO: az nem annyira jo, hogy a PartName, ContentType, stb. csak ugy nyersen allnak itt.
       :attrs {:PartName (str "/" (:file-name data)) ;; VAN slash!
               :ContentType (str (:mime-type data))}}))
   (as-> data
       (fn [output-stream]
         (println "---------")
         (clojure.pprint/pprint data)
         (println "--------")
         (let [writer (io/writer output-stream)]
           (xml/emit data writer)
           (.flush writer))))))

(comment


  (with-parts-data
    (do
      (@#'stencil.postprocess.html/register-html! (java.util.UUID/randomUUID) "LALALA")
      ((render-content-types (clojure.java.io/file "/tmp/out5/[Content_Types].xml")) *out*)))


  )

(defn assoc-extra-files [m]
  (reduce (fn [m x]

            (assoc m (:file-name x)
                   (fn [output-stream]
                     (let [writer (io/writer output-stream)]
                       (.write writer (str (:content x)))
                       (.flush writer)))))
          m @*parts-data*))
