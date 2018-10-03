(ns stencil.tree-postprocess
  "XML fa utofeldolgozasat vegzo kod."
  (:require [clojure.zip :as zip]
            [stencil.postprocess.table :refer :all]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

(set! *warn-on-reflection* true)

(defn deref-delayed-values
  "Walks the tree (Depth First) and evaluates DelayedValueMarker objects."
  [xml-tree]
  (loop [loc (xml-zip xml-tree)]
    (if (zip/end? loc)
      (zip/root loc)
      (if (instance? clojure.lang.IDeref (zip/node loc))
        (recur (zip/next (zip/edit loc deref)))
        (recur (zip/next loc))))))

(def postprocess (comp deref-delayed-values fix-tables))

:ok