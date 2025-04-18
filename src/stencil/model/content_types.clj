(ns stencil.model.content-types
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :refer [file input-stream]]
            [stencil.fs :as fs]
            [stencil.model.common :refer [->xml-writer]]))

(set! *warn-on-reflection* true)

(def xmlns "http://schemas.openxmlformats.org/package/2006/content-types")

(def tag-types :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Fcontent-types/Types)
(def tag-override :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Fcontent-types/Override)
(def tag-default :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Fcontent-types/Default)

(def attr-extension :Extension)
(def attr-part-name :PartName)
(def attr-content-type :ContentType)


(defn- parse-ct-file [content-types-file]
  (with-open [reader (input-stream (file content-types-file))]
    (let [parsed (xml/parse reader)]
      (assert (= "Types" (name (:tag parsed))))
      (reduce (fn [m elem]
                (case (name (:tag elem))
                  "Default"  (assoc-in m [::default (attr-extension (:attrs elem))] (attr-content-type (:attrs elem)))
                  "Override" (assoc-in m [::override (attr-part-name (:attrs elem))] (attr-content-type (:attrs elem)))))
              {} (remove string? (:content parsed)))))) ;; rm empty strings


(defn parse-content-types [dir]
  (assert (fs/directory? dir))
  (let [cts (file dir "[Content_Types].xml")]
    (assert (fs/exists? cts))
    (assert (.isFile cts))
    {:parsed                   (parse-ct-file cts)
     :stencil.model/path       (.getName cts)}))


(defn with-content-types [model]
  (let [parsed (-> model :content-types :parsed)
        tree   {:tag tag-types
                :attrs {:xmlns xmlns}
                :content (concat (for [[k v] (::default parsed)]
                                   {:tag tag-default :attrs {attr-extension k attr-content-type v}})
                                 (for [[k v] (::override parsed)]
                                   {:tag tag-override :attrs {attr-part-name k attr-content-type v}}))}]
    (assoc-in model [:content-types :result :writer] (->xml-writer tree))))


(defn assoc-override [model path mime-type]
  (assoc-in model [:content-types :parsed ::override path] mime-type))
