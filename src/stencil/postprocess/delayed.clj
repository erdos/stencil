(ns stencil.postprocess.delayed
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

(defn- dfs-walk-xml [xml-tree predicate edit-fn]
  (loop [loc (xml-zip xml-tree)]
    (if (zip/end? loc)
      (zip/root loc)
      (if (predicate (zip/node loc))
        (recur (zip/next (zip/edit loc edit-fn)))
        (recur (zip/next loc))))))

(defn deref-delayed-values
  "Walks the tree (Depth First) and evaluates DelayedValueMarker objects."
  [xml-tree]
  (dfs-walk-xml xml-tree (partial instance? clojure.lang.IDeref) deref))
