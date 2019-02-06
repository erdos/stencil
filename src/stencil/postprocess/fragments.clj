(ns stencil.postprocess.fragments
  "Calls deref on delayed values in an XML tree."
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

#_
(defn- unpack-fragment [tree-loc]
  (zip/replace tree-loc "ASDF"))

#_
(defn unpack-fragments
  "Walks the tree (Depth First) and evaluates DelayedValueMarker objects."
  [xml-tree]
  (dfs-walk-xml xml-tree (partial instance? FragmentInvoke) unpack-fragment))
