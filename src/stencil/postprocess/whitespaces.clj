(ns stencil.postprocess.whitespaces
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

;;
;;
;;
;; http://officeopenxml.com/WPtext.php

;; like clojure.walk/postwalk but keeps metadata and calls fn only on nodes
;; also: explicitly keeps meta data
(defn- postwalk-xml [f xml-tree]
  (if (map? xml-tree)
    (with-meta
      (f (update xml-tree :content (partial mapv (partial postwalk-xml f))))
      (meta xml-tree))
    xml-tree))

(def ooxml-t :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/t)
(def ooxml-attr-space :xmlns.http%3A%2F%2Fwww.w3.org%2FXML%2F1998%2Fnamespace/space)

(defn- should-fix?
  "We only fix <t> tags where the enclosed string starts or ends with whitespace."
  [element]
  (boolean
   (when (and (map? element)
              (= ooxml-t (:tag element))
              (seq (:content element)))
     (or (.startsWith (str (first (:content element))) " ")
         (.startsWith (str (last (:content element))) " ")))))

(defn- fix-elem [element]
  (assoc-in element [:attrs ooxml-attr-space] "preserve"))

(defn fix-whitespaces [xml-tree]
  (postwalk-xml #(if (should-fix? %) (fix-elem %) %) xml-tree))
