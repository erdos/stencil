(ns stencil.api
  "A simple public API for document generation from templates."
  (:import [io.github.erdos.stencil API PreparedTemplate TemplateData]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn prepare
  "Creates a prepared template instance from an input document."
  [input]
  (cond
    (instance? PreparedTemplate input) input
    (nil? input)  (throw (ex-info "Template is missing!" {}))
    :otherwise    (API/prepare (clojure.java.io/file input))))

(defn- make-template-data [x]
  (if (map? x)
    (TemplateData/fromMap ^java.util.Map x)
    (throw (ex-info (str "Unsupported template data type " (type x) "!")
                    {:template-data x}))))

(defn render!
  "Takes a prepared template instance and renders it.
   By default it returns an InputStream of the rendered document."
  [template template-data & {:as opts}]
  (let [template      (prepare template)
        template-data (make-template-data template-data)]
    (API/render template ^TemplateData template-data)))
