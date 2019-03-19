(ns stencil.postprocess.html
  "Replaces results of html() calls with external part relationships."
  (:require [clojure.zip :as zip]
            [clojure.data.xml :as xml]
            [stencil.functions :refer [call-fn]]
            [stencil.types :refer [control?]]
            [stencil.util :refer :all]
            [stencil.ooxml :as ooxml]))

(defrecord HtmlChunk [content])
(defmethod control? HtmlChunk [x] true)
(defmethod call-fn "html" [_ content] (->HtmlChunk content))

(def legal-tags
  "Set of supported HTML tags"
  #{:b :em :i :u :s :sup :sub :span :br :strong})

(defn- validate-tags
  "Throws ExceptionInfo on invalid HTML tag in tree"
  [xml-tree]
  (->>
   (fn [node]
     (if (legal-tags (:tag node))
       node
       (throw (ex-info (str "Unexpected HTML tag: " (:tag node)) {:tag (:tag node)}))))
   (dfs-walk-xml xml-tree map?)))

(defn- parse-html [xml]
  (-> (str xml)
      (.replaceAll "<br>" "<br/>")
      (xml/parse-str)
      (doto (validate-tags))
      (try (catch javax.xml.stream.XMLStreamException e
             (throw (ex-info "Invalid HTML content!" {:raw-xml xml} e))))))

(defn- walk-children [xml]
  (if (map? xml)
    (if (= :br (:tag xml))
      [{:text ::br}]
      (for [c (:content xml)
            x (walk-children c)]
        (update x :path conj (:tag xml))))
    [{:text xml}]))

(defn- path->styles [path]
  (cond-> []
    (some #{:b :em :strong} path) (conj {:tag ooxml/b :attrs {ooxml/val "true"}})
    (some #{:i} path) (conj {:tag ooxml/i :attrs {ooxml/val "true"}})
    (some #{:s} path) (conj {:tag ooxml/strike :attrs {ooxml/val "true"}})
    (some #{:u} path) (conj {:tag ooxml/u :attrs {ooxml/val "single"}})
    (some #{:sup} path) (conj {:tag ooxml/vertAlign :attrs {ooxml/val "superscript"}})
    (some #{:sub} path) (conj {:tag ooxml/vertAlign :attrs {ooxml/val "subscript"}})))

(defn html->ooxml-runs
  "Parses html string and returns a seq of ooxml run elements.
   Parameter base-style is the default styling for each run."
  [html base-style]
  (when (seq html)
    (let [ch (walk-children (parse-html (str "<span>" html "</span>")))]
      (for [parts (partition-by :path ch)
            :let [prs (into (set base-style) (path->styles (:path (first parts))))]]
        {:tag ooxml/r
         :content (cons {:tag ooxml/rPr :content (vec prs)}
                        (for [{:keys [text]} parts]
                          (if (= ::br text)
                            {:tag ooxml/br :content []}
                            {:tag ooxml/t :content [(str text)]})))}))))

;; splits enclosing t and r tags
(defn- fix-html-chunk [chunk-loc]
  (assert (instance? HtmlChunk (zip/node chunk-loc)))
  (let [lefts (zip/lefts chunk-loc)
        rights (zip/rights chunk-loc)

        t    (zip/node (zip/up chunk-loc))
        r    (zip/node (zip/up (zip/up chunk-loc)))

        ;;  t elems
        lefts1 (remove (comp #{ooxml/rPr} :tag) (zip/lefts (zip/up chunk-loc)))
        rights1 (zip/rights (zip/up chunk-loc))

        style (some #(when (= ooxml/rPr (:tag %)) %) (:content r))
        ooxml-runs (html->ooxml-runs (:content (zip/node chunk-loc)) (:content style))

        ->t (fn [xs] {:tag ooxml/t :content (vec xs)})
        ->run (fn [cts] (assoc r :content (vec (cons style cts))))]
    (assert (= ooxml/t (:tag t)))
    (assert (= ooxml/r (:tag r)))
    (-> chunk-loc
        (zip/up) ;; t
        (zip/up) ;; r

        (cond-> (seq lefts1) (zip/insert-left (->run lefts1)))
        (cond-> (seq lefts) (zip/insert-left (->run [(->t lefts)])))

        (cond-> (seq rights1) (zip/insert-right (->run rights1)))
        (cond-> (seq rights) (zip/insert-right (->run [(->t rights)])))

        (as-> * (reduce zip/insert-right * (reverse ooxml-runs)))

        (zip/remove))))

(defn fix-html-chunks [xml-tree]
  (dfs-walk-xml-node xml-tree #(instance? HtmlChunk %) fix-html-chunk))
