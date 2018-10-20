(ns stencil.api-test
  (:require [stencil.api :refer :all]))

(comment (def template-1 (prepare "/home/erdos/Joy/stencil/test-resources/test-control-conditionals.docx"))

         (defn render-template-1 [output-file data]
           (render! template-1 data :output output-file))

         (render-template-1 "/tmp/output-3.docx" {"customerName" "John Doe"}))
