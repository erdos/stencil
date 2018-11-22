(ns stencil.tree-postprocess
  "Postprocessing an xml tree"
  (:require [stencil.postprocess.delayed :refer :all]
            [stencil.postprocess.table :refer :all]
            [stencil.postprocess.whitespaces :refer :all]
            [stencil.postprocess.ignored-tag :refer :all]
            [stencil.postprocess.html :refer :all]))

;; calls postprocess
(def postprocess
  (comp deref-delayed-values
        fix-tables
        fix-whitespaces
        unmap-ignored-attr
        fix-html-chunks))
