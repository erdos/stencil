(ns stencil.postprocess.fragments
  "Calls deref on delayed values in an XML tree."
  (:import [stencil.types FragmentInvoke])
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

(defn- unpack-fragment [tree-loc]
  (zip/replace tree-loc "ASDF")
  ;; itt azt kellene, hogy elindulunk folfele es kettevagjuk a p es t tagokat.
  ;; a kettevagott p tag koze beszurjuk a fragment meghivasabol kapott reszeket.
  ;;
  ;; hasonlo kod kell, mint a fix-html-chunk fuggveny!
  )

(defn unpack-fragments
  "Walks the tree (Depth First) and evaluates DelayedValueMarker objects."
  [xml-tree]
  (dfs-walk-xml xml-tree (partial instance? FragmentInvoke) unpack-fragment))
