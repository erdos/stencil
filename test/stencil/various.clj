(ns stencil.various
  "Not used yet."
  (:require [clojure.data.xml.pu-map :as pu-map]
            [clojure.string :as s]
            [stencil.postprocess.ignored-tag :refer :all]))

(defn- url-decode [s] (java.net.URLDecoder/decode (str s) "UTF-8"))

(defn- tag-and-attrs-namespaces [form]
  (when (map? form)
    (for [x (keep namespace (list* (:tag form) (keys (:attrs form))))
          :when (.startsWith (str x) "xmlns.")]
      (url-decode (.substring (str x) 6)))))

(defn- elem-meta-namespaces [form]
  (when (map? form)
    (->> form meta :clojure.data.xml/nss :u->ps keys
         (remove #{"http://www.w3.org/2000/xmlns/"
                   "http://www.w3.org/XML/1998/namespace"}))))

(defn- collect-all-nss [form]
  (->> form
       (tree-seq map? :content)
       (map (juxt tag-and-attrs-namespaces elem-meta-namespaces))
       (flatten)
       (into (sorted-set))))
