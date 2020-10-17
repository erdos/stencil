(ns stencil.zap
  (:require [stencil.util :refer :all]
            [clojure.zip :as zip]))

;; zipper utilties

(defn zap* [xml & filters]
  (let [xml (if (zipper? xml) xml (xml-zip xml))]
    (reduce
     (fn [xml item]
       (or
        (cond (= 'LEFT item)  (zip/left xml)
              (= 'RIGHT item) (zip/right xml)
              (= 'UP item)    (zip/up xml)
              (= 'DOWN item)  (zip/down xml)
              (= 'NEXT item)  (zip/next xml)
              (= 'PREV item)  (zip/prev xml)

              :else (assert false))
        (reduced nil))) xml filters)))

(defmacro zap [xml & filters] `(zap* ~xml ~@(for [f filters] (list 'quote f))))

(comment

  (zap tree

       RIGHT
       UP

       (FILTER-TAG "ASD")
       (FILTER-ATTR "sadf" "")

       )


  )
