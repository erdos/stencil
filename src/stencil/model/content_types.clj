(ns stencil.model.content-types
  (:require [clojure.java.io :refer [file]]
            [clojure.data.xml :as xml]
            [clojure.java.io :refer [file input-stream]]
            [stencil.ooxml :as ooxml]
            [stencil.model.common :refer [->xml-writer]])
  (:import [java.io File]))

(def xmlns "http://schemas.openxmlformats.org/package/2006/content-types")

(def tag-types :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Fcontent-types/Types)
(def tag-override :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Fcontent-types/Override)
(def tag-default :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Fcontent-types/Default)

(def attr-extension :Extension)
(def attr-part-name :PartName)
(def attr-content-type :ContentType)

;; return a map of 
(defn- parse-ct-file [content-types-file]
  (with-open [reader (input-stream (file content-types-file))]
    (let [parsed (xml/parse reader)]
      (assert (= "Types" (name (:tag parsed))))
      (reduce (fn [m elem]
                ;(println :! (:tag elem) elem (pr-str elem))
                (println :! (:attrs elem))
                (case (name (:tag elem))
                  "Default"  (assoc-in m [::default (attr-extension (:attrs elem))] (attr-content-type (:attrs elem)))
                  "Override" (assoc-in m [::override (attr-part-name (:attrs elem))] (attr-content-type (:attrs elem)))))
              {} (remove string? (:content parsed)))))) ;; rm empty strings

(defn parse-content-types [^File dir]
  (assert (.isDirectory dir))
  (let [cts (file dir "[Content_Types].xml")]
    (assert (.exists cts))
    (assert (.isFile cts))
    {;:source-file cts
     :parsed                   (parse-ct-file cts)
     :stencil.model/path       (.getName cts)}))

(defn with-content-types [model]
  (let [parsed (-> model :content-types :parsed)
        _ (println :! parsed)
        tree   {:tag tag-types
                :attrs {:xmlns xmlns}
                :content (concat (for [[k v] (::default parsed)]
                                   {:tag tag-default :attrs {attr-extension k attr-content-type v}})
                                 (for [[k v] (::override parsed)]
                                   {:tag tag-override :attrs {attr-part-name k attr-content-type v}}))}]
    (assoc-in model [:content-types :result :writer] (->xml-writer tree))))
