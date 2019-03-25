(ns stencil.tree-postprocess
  "Postprocessing an xml tree"
  (:require [stencil.postprocess.delayed :refer :all]
            [stencil.postprocess.table :refer :all]
            [stencil.postprocess.whitespaces :refer :all]
            [stencil.postprocess.ignored-tag :refer :all]
            [stencil.postprocess.fragments :refer :all]
            [stencil.postprocess.html :refer :all]))

;; calls postprocess
(def postprocess
  (comp unmap-ignored-attr ;; must be called last
        deref-delayed-values
        fix-tables
        fix-whitespaces
        fix-html-chunks
        unpack-fragments))
