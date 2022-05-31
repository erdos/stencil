(ns stencil.postprocess.images
  (:require [clojure.java.io :as io]
            [clojure.string :refer [starts-with? lower-case]]
            [clojure.zip :as zip]
            [stencil.log :as log]
            [stencil.ooxml :as ooxml]
            [stencil.model.relations :refer [rel-type-image]]
            [stencil.util :refer [fail find-first iterations dfs-walk-xml-node]])
  (:import [stencil.types ReplaceImage]))

(set! *warn-on-reflection* true)

(def mime-type->extension
  {"image/png"  "png"
   "image/jpeg" "jpeg"
   "image/bmp"  "bmp"
   "image/gif"  "gif"})

(defmulti parse-data-uri type)

(defmethod parse-data-uri :default [value]
  (fail "Can not parse image from template data!" {:type (type value)}))

(defmethod parse-data-uri String [^String data-uri-str]
  (when-not (starts-with? data-uri-str "data:")
    (fail "Image data should be a valid data uri!" {}))
  (let [end-of-mimetype (.indexOf data-uri-str ";")
        start-of-data   (inc (.indexOf data-uri-str ","))]
    (when-not (and (pos? start-of-data)
                   (= "base64" (subs data-uri-str (inc end-of-mimetype) (dec start-of-data))))
      (fail "Image data should be in valid base64-encoded data uri format!" {}))
    {:mime-type (lower-case (subs data-uri-str 5 end-of-mimetype))
     :bytes     (.decode (java.util.Base64/getDecoder) (.getBytes (subs data-uri-str start-of-data)))}))

(defn- update-image [img-node, ^ReplaceImage data]
  (assert (= ooxml/blip (:tag img-node)))
  (assert (instance? ReplaceImage data))
  (let [current-rel (-> img-node :attrs ooxml/r-embed)
        new-val     (-> data .relation)]
    (assert new-val)
    (log/debug "Replacing image relation {} by {}" current-rel new-val)
    (assoc-in img-node [:attrs ooxml/r-embed] new-val)))

(defn- replace-image [marker-loc]
  (if-let [img-loc (->> (zip/remove marker-loc)
                        (iterations zip/prev)
                        (find-first (comp #{ooxml/blip} :tag zip/node)))]
    (zip/edit img-loc update-image (zip/node marker-loc))
    (fail "Did not find image to replace. The location of target image must precede the replaceImage() function call location." {})))

(defn replace-images [xml-tree]
  (dfs-walk-xml-node
   xml-tree
   (partial instance? ReplaceImage)
   replace-image))

(defn- ->relation-id [] (str (gensym "srel")))

(defn- bytes->writer [^bytes bytes]
  (fn [writer]
    (doto (io/output-stream writer)
      (.write bytes)
      (.flush))
    nil))

(defn- image-path [rel-id mime-type]
  (if-let [extension (mime-type->extension mime-type)]
    (str "media/" rel-id "." extension)
    (fail "Unexpected mime-type for image!" {:mime-type mime-type})))

(defn img-data->extrafile [data-uri]
  (let [new-rel                   (->relation-id)
        {:keys [mime-type bytes]} (parse-data-uri data-uri)]
    {:new-id               new-rel
     :stencil.model/type   rel-type-image
     :stencil.model/target (image-path new-rel mime-type)
     :writer               (bytes->writer bytes)}))
