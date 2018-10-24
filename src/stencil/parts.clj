(ns stencil.parts
  "This ns is used to addeg generated content as parts of DOCX files."
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [stencil.util :refer :all]))

(defn parse-data-uri [^String data-uri-str]
  (assert (string? data-uri-str))
  (assert (.startsWith data-uri-str "data:"))
  (let [end-of-mimetype (.indexOf data-uri-str ";")
        start-of-data   (inc (.indexOf data-uri-str ","))
        raw-data  (.substring data-uri-str start-of-data)]
    (assert (= "base64" (.substring data-uri-str (inc end-of-mimetype) (dec start-of-data))))
    {:mime-type (.toLowerCase (.substring data-uri-str 5 end-of-mimetype))
     ;; :raw-data raw-data
     :data      (String. (.decode (java.util.Base64/getDecoder) (.getBytes raw-data)))}))

; (parse-data-uri "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/4gKgSUNDX1BST0ZJTEUAAQEAAAKQbGNtcwQwAABtbnRyUkdCIFhZWiAH4QAGAAcAEAAoAB1hY3NwQVBQTAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA9tYAAQAAAADTLWxjbXMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAtkZXNjAAABCAAAADhjcHJ0AAABQAAAAE53dHB0AAABkAAAABRjaGFkAAABpAAAACxyWFlaAAAB0AAAABRiWFlaAAAB5AAAABRnWFlaAAAB+AAAABRyVFJDAAACDAAAACBnVFJDAAACLAAAACBiVFJDAAACTAAAACBjaHJtAAACbAAAACRtbHVjAAAAAAAAAAEAAAAMZW5VUwAAABwAAAAcAHMAUgBHAEIAIABiAHUAaQBsAHQALQBpAG4AAG1sdWMAAAAAAAAAAQAAAAxlblVTAAAAMgAAABwATgBvACAAYwBvAHAAeQByAGkAZwBoAHQALAAgAHUAcwBlACAAZgByAGUAZQBsAHkAAAAAWFlaIAAAAAAAAPbWAAEAAAAA0y1zZjMyAAAAAAABDEoAAAXj///zKgAAB5sAAP2H///7ov///aMAAAPYAADAlFhZWiAAAAAAAABvlAAAOO4AAAOQWFlaIAAAAAAAACSdAAAPgwAAtr5YWVogAAAAAAAAYqUAALeQAAAY3nBhcmEAAAAAAAMAAAACZmYAAPKnAAANWQAAE9AAAApbcGFyYQAAAAAAAwAAAAJmZgAA8qcAAA1ZAAAT0AAACltwYXJhAAAAAAADAAAAAmZmAADypwAADVkAABPQAAAKW2Nocm0AAAAAAAMAAAAAo9cAAFR7AABMzQAAmZoAACZmAAAPXP/bAEMABQMEBAQDBQQEBAUFBQYHDAgHBwcHDwsLCQwRDxISEQ8RERMWHBcTFBoVEREYIRgaHR0fHx8TFyIkIh4kHB4fHv/bAEMBBQUFBwYHDggIDh4UERQeHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh")


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
    :content into
    (for [data @*parts-data*]
      {:tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationship
       :attrs {:Id     (str (:id data))
               :Target (str (:file-name data)) ;; nincs per
               :Type   (:rel-type data)
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
    :content into
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
