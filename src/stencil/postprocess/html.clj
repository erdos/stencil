(ns stencil.postprocess.html
  "Replaces results of html() calls with external part relationships."
  (:require [clojure.zip :as zip]
            [clojure.data.xml :as xml]
            [stencil.functions :refer [call-fn]]
            [stencil.types :refer [control?]]
            [stencil.util :refer :all]))

;; run and text
(def ooxml-r :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/r)
(def ooxml-rPr :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/rPr)
(def ooxml-t :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/t)

(def ooxml-p :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/p)

(def ooxml-val :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/val)

;; are they real?
(def ooxml-b :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/b)
(def ooxml-i :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/i)
(def ooxml-strike :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/strike)
(def ooxml-u :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/u)

(defrecord HtmlChunk [content])
(defmethod control? HtmlChunk [x] true)
(defmethod call-fn "html" [_ content] (->HtmlChunk content))


(defn- find-enclosing-p [loc]
  (find-first (comp #{ooxml-p} :tag zip/node) (take-while some? (iterations zip/up loc))))

(def legal-tags #{"b" "i" "u" "s" "sup" "sub"})

;; throws an exception on invalid xml tags.
(defn- validate-tags [xml-tree])

(defn- parse-html [xml]
  (doto (xml/parse-str (.replaceAll (str xml) "<br>" "<br/>"))
    (validate-tags)))

(def block-tags #{:div :p})



(def prop-b {:tag ooxml-b :attrs {ooxml-val "true"}})
(def prop-i {:tag ooxml-i :attrs {ooxml-val "true"}})
(def prop-s {:tag ooxml-strike :attrs {ooxml-val "true"}})
(def prop-u {:tag ooxml-u :attrs {ooxml-val "single"}})

(def prop-sup {:tag :w/vertAlign :attrs {ooxml-val "superscript"}})
(def prop-sub {:tag :w/vertAlign :attrs {ooxml-val "subscript"}})

(defn- color [c] {:tag :w/color :attrs {ooxml-val (str c)}})


(defn- walk-children [xml]
  (if (map? xml)
    (if (= :br (:tag xml))
      [{:text ::br}]
      (for [c (:content xml)
            x (walk-children c)]
        (update x :path conj (:tag xml))))
    [{:text xml}]))

;; (walk-children (parse-html test-text))


(defn html->ooxml [html]
  (when (seq html)
    (let [ch (walk-children (parse-html (str "<span>" html "</span>")))]
      (for [{:keys [text path]} ch]

        (if (= ::br text)
          {:tag :w/br}
          (let [prs (cond-> [] ;; style information
                      (some (comp #{:b} :tag) path)   (conj prop-b)
                      (some (comp #{:i} :tag) path)   (conj prop-i)
                      (some (comp #{:s} :tag) path)   (conj prop-s)
                      (some (comp #{:u} :tag) path)   (conj prop-u)
                      (some (comp #{:sup} :tag) path) (conj prop-sup)
                      (some (comp #{:sub} :tag) path) (conj prop-sub))]
            {:tag :w/p
             :content [
                       {:tag :w/pPr :content prs}
                       {:tag :w/r :content [{:tag :w/t :content [(str text)]}]}]}))
        ))))


(comment ;;
  (let [test-text "<span>Hello <u>Vilag!</u> Mi a <b>helyzet<i>?</i></b></span>"]
    (html->ooxml test-text))

  )

(declare inline?) ;; el kell majd tudni donteni h a beszurando html inline v sem.

(defn find-parent-run [chunk-loc])
(declare find-run-props-seq)

(defn split-run [chunk-loc]
  (let [lefts (zip/lefts chunk-loc)
        rights (zip/rights chunk-loc)
        html (zip/node chunk-loc)
        t    (zip/node (zip/up chunk-loc))
        r    (zip/node (zip/up (zip/up chunk-loc)))

        style (some #(when (= ooxml-rPr (:tag %)) %) (:content r))
        run (fn [cts] (-> r
                        (assoc :content [style])
                        (update :content concat cts)))
        ]
    (assert (= ooxml-t (:tag t)) (str (:tag t)))
    (assert (= ooxml-r (:tag r)))
    (-> chunk-loc
       (zip/up) ;; t
       (zip/up) ;; r
       (zip/replace (run [{:tag ooxml-t :content [(:content html)]}])) ;; replace run with single content
       ;; todo: maybe partition it too

       (cond-> (seq lefts) (zip/insert-left (run (assoc t :content lefts))))
       (cond-> (seq rights) (zip/insert-right (run (assoc t :content rights))))
       )))

(comment ;;

  (defn <p> [& contents] {:tag ooxml-p :content contents})
  (defn <r> [& contents] {:tag ooxml-r :content contents})
  (defn <rPr> [& contents] {:tag ooxml-rPr :content contents})

  (defn <t> [& contents] {:tag ooxml-t :content contents})



  )

(defn- fix-html-chunk [chunk-loc]
  #_(let [parent-run (find-parent-run chunk-loc)
        parent-props (find-run-props-seq)])
  (split-run chunk-loc)
  )

(defn fix-html-chunks [xml-tree]
  (dfs-walk-xml-node xml-tree #(instance? HtmlChunk %) fix-html-chunk))

;; (stencil.parts/with-parts-data (fix-html-chunks {:tag ooxml-p :content [(->HtmlChunk "asd")] :attrs {}}))


(fix-html-chunks
 (<p> (<r>
       (<rPr> :x :y)
       (<t> "Elotte1")
       (<t> "E1" (->HtmlChunk "ala") "E2")
       (<t> "Utana"))))
