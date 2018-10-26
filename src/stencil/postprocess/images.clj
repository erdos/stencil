(ns stencil.postprocess.images
  (:require [clojure.zip :as zip]
            [stencil.parts :refer [register-external!]]
            [stencil.util :refer :all]))

(def ooxml-nvpr :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fdrawingml%2F2006%2Fpicture/cNvPr)

(defrecord ImageData [mime-type data])

(defn parse-data-uri [^String data-uri-str]
  (assert (string? data-uri-str))
  (assert (.startsWith data-uri-str "data:"))
  (let [end-of-mimetype (.indexOf data-uri-str ";")
        start-of-data   (inc (.indexOf data-uri-str ","))
        raw-data  (.substring data-uri-str start-of-data)]
    (assert (= "base64" (.substring data-uri-str (inc end-of-mimetype) (dec start-of-data))))
    {:mime-type (.toLowerCase (.substring data-uri-str 5 end-of-mimetype))
     :data      (new java.io.ByteArrayInputStream
                     (.decode (java.util.Base64/getDecoder) (.getBytes raw-data)))}))

#_
(defmethod eval-step
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fdrawingml%2F2006%2Fpicture/cNvPr
  [data function element]
  (let [name (-> element :attrs :name)]
    (if (vector? name)
      (assoc-in element [:attrs :name]
                #(map->ImageData (parse-data-uri (eval-rpn data function %))))
      element)))

(def mime-type->extension
  {"image/png"  "png"
   "image/jpeg" "jpeg"
   "image/bmp"  "bmp"
   "image/gif"  "gif"})

(declare find-blip-elem)

(defn insert-image [img-loc]
  (let [evaled      (some-> zip/node img-loc :attrs :name parse-data-uri)
        id          (java.util.UUID/randomUUID)
        new-name    "Dynamic Image" ;; todo: maybe numbering here
        ]
    (if-not evaled
      img-loc ;; ha nem sikerult evalualni, akkor nem csinalunk vele semmit
      (do
        (register-external!
         :id id
         :file-name (str id "." (mime-type->extension (:mime-type evaled)))
         :rel-type "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image"
         ;; :rel-target-mode DIREKT nincs kitoltve.
         :content (:data evaled)
         :mime-type (:mime-type evaled))
        (-> img-loc
           (zip/edit assoc-in [:attrs :name] new-name)
           (zip/up) (zip/up) ;; <pic> element
           (find-blip-elem)
           (zip/edit assoc-in [:attrs :embed] (str id)))))))

(defn- dynamic-image? [elem] (some->> elem :attrs :name (instance? ImageData)))

(defn fix-html-chunks [xml-tree]
  (dfs-walk-xml-node xml-tree dynamic-image? insert-image))


;; (parse-data-uri "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/4gKgSUNDX1BST0ZJTEUAAQEAAAKQbGNtcwQwAABtbnRyUkdCIFhZWiAH4QAGAAcAEAAoAB1hY3NwQVBQTAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA9tYAAQAAAADTLWxjbXMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAtkZXNjAAABCAAAADhjcHJ0AAABQAAAAE53dHB0AAABkAAAABRjaGFkAAABpAAAACxyWFlaAAAB0AAAABRiWFlaAAAB5AAAABRnWFlaAAAB+AAAABRyVFJDAAACDAAAACBnVFJDAAACLAAAACBiVFJDAAACTAAAACBjaHJtAAACbAAAACRtbHVjAAAAAAAAAAEAAAAMZW5VUwAAABwAAAAcAHMAUgBHAEIAIABiAHUAaQBsAHQALQBpAG4AAG1sdWMAAAAAAAAAAQAAAAxlblVTAAAAMgAAABwATgBvACAAYwBvAHAAeQByAGkAZwBoAHQALAAgAHUAcwBlACAAZgByAGUAZQBsAHkAAAAAWFlaIAAAAAAAAPbWAAEAAAAA0y1zZjMyAAAAAAABDEoAAAXj///zKgAAB5sAAP2H///7ov///aMAAAPYAADAlFhZWiAAAAAAAABvlAAAOO4AAAOQWFlaIAAAAAAAACSdAAAPgwAAtr5YWVogAAAAAAAAYqUAALeQAAAY3nBhcmEAAAAAAAMAAAACZmYAAPKnAAANWQAAE9AAAApbcGFyYQAAAAAAAwAAAAJmZgAA8qcAAA1ZAAAT0AAACltwYXJhAAAAAAADAAAAAmZmAADypwAADVkAABPQAAAKW2Nocm0AAAAAAAMAAAAAo9cAAFR7AABMzQAAmZoAACZmAAAPXP/bAEMABQMEBAQDBQQEBAUFBQYHDAgHBwcHDwsLCQwRDxISEQ8RERMWHBcTFBoVEREYIRgaHR0fHx8TFyIkIh4kHB4fHv/bAEMBBQUFBwYHDggIDh4UERQeHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh")
