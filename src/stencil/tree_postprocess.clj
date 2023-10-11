(ns stencil.tree-postprocess
  "Postprocessing an xml tree"
  (:require [stencil.postprocess.table :refer :all]
            [stencil.postprocess.whitespaces :refer :all]
            [stencil.postprocess.ignored-tag :refer :all]
            [stencil.postprocess.images :refer :all]
            [stencil.postprocess.links :refer :all]
            [stencil.postprocess.list-ref :refer :all]
            [stencil.postprocess.fragments :refer :all]
            [stencil.postprocess.html :refer :all]))

;; calls postprocess
(def postprocess
  (comp

   ;; must be called last. replaces the Ignored attrubute values from ids to namespaces.
   #'unmap-ignored-attr

   ;; hides rows/columns where markers are present
   #'fix-tables

   ;; fixes xml:space attribute values where missing
   #'fix-whitespaces

   ;; includes html() call results.
   #'fix-html-chunks

   #'fix-list-dirty-refs

   #'replace-images

   #'replace-links

   ;; call this first. includes fragments and evaluates them too.
   #'unpack-fragments))
