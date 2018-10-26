(ns stencil.postprocess.html
  "Replaces results of html() calls with external part relationships."
  (:require [clojure.zip :as zip]
            [stencil.functions :refer [call-fn]]
            [stencil.types :refer [control?]]
            [stencil.parts :refer [register-external!]]
            [stencil.util :refer :all]))

(def ooxml-p :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/p)

(defrecord HtmlChunk [content])
(defmethod control? HtmlChunk [x] true)

(defmethod call-fn "html" [_ content] (->HtmlChunk content))

;; egy lehetseges algoritmus:
;; felparszolja az xml-t. szelessegi bejarassal vegigmegy a gyermek csomopontokon.
;; minden csomoponton elindul folfele es osszegyujti az ervenyes formazasokat
;; ezek alapjan letrehozza a megfelelo formazasi run hivasokat.
;; ha kulon p-ben vagy div-ben vannak akkor az kulon bekezdesbe kerul
;; ha <font> tag van, akkor a size/color is figyelembe lesz veve
;; ha <i> <b> (<em>) <u> <s> tagek vannak akkor azok a stilust modositjak




(defn ->html-chunk [id]
  {:tag :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/altChunk
   :attrs {:xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/id (str id)}
   :content []})

(defn- find-enclosing-p [loc]
  (find-first (comp #{ooxml-p} :tag zip/node) (take-while some? (iterations zip/up loc))))

(defn- register-html! [id content]
  (assert id)
  (assert (string? content))
  (let [id (.replace (str id) "-" "")]
    (register-external!
     :id id
     :file-name (str "word/" id ".htm")
     :rel-type "http://schemas.openxmlformats.org/officeDocument/2006/relationships/aFChunk"
                                        ;          http://schemas.openxmlformats.org/officeDocument/2006/relationships/afChunk
     :rel-target-mode "Internal"

     ;;  TODO: ok ez lehet mashogy is
     :content (str "<html><body>" content "</body></html>")
     :mime-type "application/xhtml+xml")))

(defn- fix-html-chunk [chunk-loc]
  (let [id (str "r" (java.util.UUID/randomUUID))]
    (if-let [chunk-p-parent (find-enclosing-p chunk-loc)]
      ;; then finds its <p> parent and replace it with a ->html-chunk call
      (do (register-html! id (:content (zip/node chunk-loc)))
          (zip/replace chunk-p-parent (->html-chunk id)))
      (do chunk-loc))))

(defn fix-html-chunks [xml-tree]
  (dfs-walk-xml-node xml-tree #(instance? HtmlChunk %) fix-html-chunk))

;; (stencil.parts/with-parts-data (fix-html-chunks {:tag ooxml-p :content [(->HtmlChunk "asd")] :attrs {}}))
