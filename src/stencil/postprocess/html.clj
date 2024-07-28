(ns stencil.postprocess.html
  "Replaces results of html() calls with external part relationships."
  (:require [clojure.zip :as zip]
            [clojure.data.xml :as xml]
            [stencil.functions :refer [def-stencil-fn]]
            [stencil.postprocess.fragments :as fragments]
            [stencil.types :refer [ControlMarker]]
            [stencil.util :refer [find-first dfs-walk-xml dfs-walk-xml-node]]
            [stencil.ooxml :as ooxml]))

(set! *warn-on-reflection* true)

(defrecord HtmlChunk [content] ControlMarker)

(def-stencil-fn "html"
  "It is possible to embed text with basic dynamic formatting using HTML notation.
   The HTML code will be converted to OOXML and inserted in the document.

   Stencil uses a simple parsing algorithm to convert between the formats. At the moment only a limited set of basic formatting is implemented. You can use the following HTML tags:
   - b, em, strong for bold text.
   - i for italics.
   - u for underlined text.
   - s for strikethrough text.
   - sup for superscript and sub for subscript.
   - span elements have no effects.
   - br tags can be used to insert line breaks.

   The rendering throws an exception on invalid HTML input or unexpected HTML tags.

   **Usage:** `{%=html(x) %}`"
  [content] (->HtmlChunk content))

(def legal-tags
  "Set of supported HTML tags"
  #{:b :em :i :u :s :sup :sub :span :br :strong})

(defn- kw-lowercase [kw] (-> kw name .toLowerCase keyword))

(defn- validate-tags
  "Throws ExceptionInfo on invalid HTML tag in tree"
  [xml-tree]
  (->>
   (fn [node]
     (if (legal-tags (-> node :tag kw-lowercase))
       node
       (throw (ex-info (str "Unexpected HTML tag: " (:tag node)) {:tag (:tag node)}))))
   (dfs-walk-xml xml-tree map?)))

(defn- parse-html [xml]
  (-> (str xml)
      (.replaceAll "<br>" "<BR/>")
      (.replaceAll "<BR>" "<BR/>")
      (xml/parse-str)
      (doto (validate-tags))
      (try (catch javax.xml.stream.XMLStreamException e
             (throw (ex-info "Invalid HTML content!" {:raw-xml xml} e))))))

(defn- walk-children [xml]
  (if (map? xml)
    (if (#{:br :BR} (:tag xml))
      [{:text ::br}]
      (for [c (:content xml)
            x (walk-children c)]
        (update x :path conj (kw-lowercase (:tag xml)))))
    [{:text xml}]))

(defn- path->style [p]
  (case p
    (:b :em :strong) {:tag ooxml/b :attrs {ooxml/val "true"}}
    (:i)             {:tag ooxml/i :attrs {ooxml/val "true"}}
    (:s)             {:tag ooxml/strike :attrs {ooxml/val "true"}}
    (:u)             {:tag ooxml/u :attrs {ooxml/val "single"}}
    (:sup)           {:tag ooxml/vertAlign :attrs {ooxml/val "superscript"}}
    (:sub)           {:tag ooxml/vertAlign :attrs {ooxml/val "subscript"}}
    nil))

(defn html->ooxml-runs
  "Parses html string and returns a seq of ooxml run elements.
   Parameter base-style is the default styling for each run."
  [html base-style]
  (when (seq html)
    (let [ch (walk-children (parse-html (str "<span>" html "</span>")))]
      (for [parts (partition-by :path ch)
            :let [prs (into (set base-style) (keep path->style) (:path (first parts)))]]
        {:tag ooxml/r
         :content (cons {:tag ooxml/rPr :content (vec prs)}
                        (for [{:keys [text]} parts]
                          (if (= ::br text)
                            {:tag ooxml/br :content []}
                            {:tag ooxml/t :content [(str text)]})))}))))

(defn- current-run-style [chunk-loc]
  (let [r (zip/node (zip/up (zip/up chunk-loc)))]
    (find-first #(= ooxml/rPr (:tag %)) (:content r))))

(defn- fix-html-chunk [chunk-loc]
  (assert (instance? HtmlChunk (zip/node chunk-loc)))
  (let [style      (current-run-style chunk-loc)
        ooxml-runs (html->ooxml-runs (:content (zip/node chunk-loc)) (:content style))]
    (apply fragments/unpack-items chunk-loc ooxml-runs)))

(defn fix-html-chunks [xml-tree]
  (dfs-walk-xml-node xml-tree #(instance? HtmlChunk %) fix-html-chunk))
