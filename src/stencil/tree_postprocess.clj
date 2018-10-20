(ns stencil.tree-postprocess
  "Postprocessing an xml tree"
  (:require [stencil.postprocess.delayed :refer :all]
            [stencil.postprocess.table :refer :all]
            [stencil.postprocess.whitespaces :refer :all]))

;; calls postprocess steps
(def postprocess (comp deref-delayed-values fix-tables fix-whitespaces))
