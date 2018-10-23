(ns stencil.parts
  "This ns is used to addeg generated content as parts of DOCX files."
  (:require [clojure.data.xml :as xml]
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
  `(binding [*parts-data* (atom [])] [~body @*parts-data*]))

(defn register-external! [& {:as opts}]
  (assert (:id opts))
  (assert (:file-name opts))
  (swap! *parts-data* conj opts))

;; returns an input stream
;; adds ids as relations
(defn render-stencil-relations [rels-file]
  (assert (instance? java.io.File rels-file))
  (->
   (with-open [reader (clojure.java.io/reader rels-file)]
     (xml/parse reader))
   (update
    :content into
    (for [data @*parts-data*]
      {:tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationship
       :attrs {:Id     (str (:id data))
               :Target (:file-name data)
               :Type   (:rel-type data)
               :TargetMode (:rel-target-mode data)}}))
   (xml/emit-str)
   (str)
   (.getBytes)
   (java.io.ByteArrayInputStream.)))

(defn render-content-types [content-types-file]
  (assert (instance? java.io.File content-types-file))
  (->
   (with-open [reader (clojure.java.io/reader content-types-file)]
     (xml/parse reader))
   (update
    :content into
    (for [data @*parts-data*]
      {:tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Fcontent-types/Override
       :attrs {:PartName (:file-name data), :ContentType (:mime-type data)}}))
   (xml/emit-str)
   (str)
   (.getBytes)
   (java.io.ByteArrayInputStream.)))
