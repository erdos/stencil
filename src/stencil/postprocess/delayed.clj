(ns stencil.postprocess.delayed
  "Calls deref on delayed values in an XML tree."
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

(defn deref-delayed-values
  "Walks the tree (Depth First) and evaluates DelayedValueMarker objects."
  [xml-tree]
  (dfs-walk-xml xml-tree (partial instance? clojure.lang.IDeref) deref))
