(ns stencil.postprocess.whitespaces
  (:require [clojure.zip :as zip]
            [stencil.ooxml :as ooxml]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

(defn- should-fix?
  "We only fix <t> tags where the enclosed string starts or ends with whitespace."
  [element]
  (boolean
   (when (and (map? element)
              (= ooxml/t (:tag element))
              (seq (:content element)))
     (or (.startsWith (str (first (:content element))) " ")
         (.startsWith (str (last (:content element))) " ")))))

(defn- fix-elem [element]
  (assoc-in element [:attrs ooxml/space] "preserve"))

(defn fix-whitespaces [xml-tree] (dfs-walk-xml xml-tree should-fix? fix-elem))
