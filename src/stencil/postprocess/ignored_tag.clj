(ns stencil.postprocess.ignored-tag
  "In docx files there might be an Ignored attribute which contains an XML namespace alias list.
   The contents of this attribute must be a valid ns alias list on the output document too!"
  (:require [clojure.data.xml.pu-map :as pu-map]
            [clojure.string :as s]))

(def ^:private ignorable-tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fmarkup-compatibility%2F2006/Ignorable)
(def ^:private choice-tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fmarkup-compatibility%2F2006/Choice)

;; like clojure.walk/postwalk but keeps metadata and calls fn only on nodes
(defn- postwalk-xml [f xml-tree]
  (if (map? xml-tree)
    (f (update xml-tree :content (partial mapv (partial postwalk-xml f))))
    xml-tree))

(defn- map-str [f s] (s/join " " (keep f (s/split s #"\s+"))))

(defn- gen-alias [] (name (gensym "ign")))

(defn- update-if-present [m path f] (if (get-in m path) (update-in m path f) m))

(defn- update-choice-requires
  "Updates the Requires attribute of a Choice tag with the fn"
  [elem f]
  (assert (ifn? f))
  (if (= (:tag elem) choice-tag)
    (update-in elem [:attrs :Requires] f)
    elem))

(defn- with-pu [object pu-map]
  (assert (map? pu-map))
  (assert (:tag object))
  (with-meta object
    {:clojure.data.xml/nss
     (apply pu-map/assoc pu-map/EMPTY (interleave (vals pu-map) (keys pu-map)))}))

;; first call this
(defn map-ignored-attr
  "Replaces values in ignorable-tag and requires-tag attributes to
   the namespace names they are aliased by."
  [xml-tree]
  (postwalk-xml
   (fn [form]
     (let [p->url (get-in (meta form) [:clojure.data.xml/nss :p->u])]
       (-> form
           (update-if-present [:attrs ignorable-tag] (partial map-str p->url))
           (update-choice-requires (partial map-str p->url)))))
   xml-tree))

;; last call this
(defn unmap-ignored-attr
  "Walks XML tree and replaces xml namespaces with aliases.
   Call just before serializing the XML tree."
  [xml-tree]
  (let [found (volatile! {}) ;; url -> alias mapping
        find! (fn [uri]
                (or (get @found uri)
                    (get (vswap! found assoc uri (gen-alias)) uri)))]
    (with-pu
      (postwalk-xml
       (fn [form]
         (-> form
             (update-if-present [:attrs ignorable-tag] (partial map-str find!))
             (update-choice-requires (partial map-str find!))))
       xml-tree)
      @found)))

:OK
