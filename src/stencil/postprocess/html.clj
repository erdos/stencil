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

(def legal-tags #{"b" "i" "u" "s" "sup" "sub"})

;; throws an exception on invalid xml tags.
(defn- validate-tags [xml-tree])

(defn- parse-html [xml]
  (doto (xml/parse-str (.replaceAll (str xml) "<br>" "<br/>"))
    (validate-tags)))

(def block-tags #{:div :p})

(def prop-b {:tag ooxml/b :attrs {ooxml/val "true"}})
(def prop-i {:tag ooxml/i :attrs {ooxml/val "true"}})
(def prop-s {:tag ooxml/strike :attrs {ooxml/val "true"}})
(def prop-u {:tag ooxml/u :attrs {ooxml/val "single"}})

(def prop-sup {:tag :w/vertAlign :attrs {ooxml/val "superscript"}})
(def prop-sub {:tag :w/vertAlign :attrs {ooxml/val "subscript"}})

(defn- color [c] {:tag :w/color :attrs {ooxml/val (str c)}})


(defn- walk-children [xml]
  (if (map? xml)
    (if (= :br (:tag xml))
      [{:text ::br}]
      (for [c (:content xml)
            x (walk-children c)]
        (update x :path conj (:tag xml))))
    [{:text xml}]))

;; (walk-children (parse-html test-text))

(defn html->ooxml-runs [html base-style]
  (when (seq html)
    (let [ch (walk-children (parse-html (str "<span>" html "</span>")))]
      (for [{:keys [text path]} ch]
        (if (= ::br text)
          (assert false "Not implemented!")
          ;; {:tag :w/br}
          (let [prs (cond-> (set base-style)
                      (some #{:b} path)   (conj prop-b)
                      (some #{:i} path)   (conj prop-i)
                      (some #{:s} path)   (conj prop-s)
                      (some #{:u} path)   (conj prop-u)
                      (some #{:sup} path) (conj prop-sup)
                      (some #{:sub} path) (conj prop-sub))]
            (println :path prs)
            {:tag ooxml/r
             :content [{:tag ooxml/rPr :content (vec prs)}
                       {:tag ooxml/t :content [(str text)]}]}))))))


(defn fix-html-chunk [chunk-loc]
  (assert (instance? HtmlChunk (zip/node chunk-loc)))
  (let [lefts (zip/lefts chunk-loc)
        rights (zip/rights chunk-loc)

        t    (zip/node (zip/up chunk-loc))
        r    (zip/node (zip/up (zip/up chunk-loc)))

        style (some #(when (= ooxml/rPr (:tag %)) %) (:content r))
        ooxml-runs (html->ooxml-runs (:content (zip/node chunk-loc)) (:content style))

        run (fn [cts] (assoc r :content (cons style cts)))]
    (assert (= ooxml/t (:tag t)) (str (:tag t)))
    (assert (= ooxml/r (:tag r)))
    (-> chunk-loc
       (zip/up) ;; t
       (zip/up) ;; r
       (cond-> (seq lefts) (zip/insert-left (run (assoc t :content lefts))))
       (cond-> (seq rights) (zip/insert-right (run (assoc t :content rights))))

       (as-> * (reduce zip/insert-right * ooxml-runs))
       (zip/remove)
       )))

(defn fix-html-chunks [xml-tree] (dfs-walk-xml-node xml-tree #(instance? HtmlChunk %) fix-html-chunk))
