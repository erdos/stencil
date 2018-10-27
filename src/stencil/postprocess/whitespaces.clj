(ns stencil.postprocess.whitespaces
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

;; See: http://officeopenxml.com/WPtext.php

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

(defn fix-whitespaces [xml-tree] (dfs-walk-xml xml-tree should-fix? fix-elem))
