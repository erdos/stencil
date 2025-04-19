(ns stencil.tree-postprocess
  "Postprocessing an xml tree"
  (:require [stencil.postprocess.table :refer [fix-tables]]
            [stencil.postprocess.whitespaces :refer [fix-whitespaces]]
            [stencil.postprocess.ignored-tag :refer [unmap-ignored-attr]]
            [stencil.postprocess.images :refer [replace-images]]
            [stencil.postprocess.links :refer [replace-links]]
            [stencil.postprocess.list-ref :refer [fix-list-dirty-refs]]
            [stencil.postprocess.fragments :refer [unpack-fragments]]
            [stencil.postprocess.html :refer [fix-html-chunks]]))

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
