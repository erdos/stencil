(ns stencil.api-test
  (:require [stencil.api :refer :all]))

(comment ;; try to prepare then render a DOCX file

  (def template-1 (prepare "/home/erdos/Joy/stencil/test-resources/test-control-conditionals.docx"))

  (defn render-template-1 [output-file data]
    (render! template-1 data :output output-file))

  (render-template-1 "/tmp/output-3.docx" {"customerName" "John Doe"}))

(comment ;; try to prepare then render a PPTX presentation file

  (def template-2 (prepare "/home/erdos/example-presentation.pptx"))

  (defn render-template-2 [output-file data]
    (render! template-2 data :output output-file))

  (render-template-2 "/tmp/output-7.pptx"
                     {"customerName" "John Doe" "x" "XXX" "y" "yyyy"}))

(comment
  (->
   "/home/erdos/Work/moby-aegon/templates/stencil/AJANLATI_NYOMTATVANY.docx"
   (prepare)
   (time)
   (render! {:packageCoverSumStructure [{:pns {:pn1 "a" :pn2 "b" :pn3 "c"}}
                                        {:pns {:pn1 "x" :pn2 "y"}}
                                        {:pns {:pn1 "p"}}]}
            :output "/tmp/out1.docx"
            :overwrite? true)
   (time))

  comment)
