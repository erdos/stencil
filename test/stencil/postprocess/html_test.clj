(ns stencil.postprocess.html-test
  (:require [clojure.zip :as zip]
            [clojure.data.xml :as xml]))

;; TODO: take extra care for whitespaces!

;; supported tags: i, u, s, b, sup, sub

(def test-text "<span>Hello <u>Vilag!</u> Mi a <b>helyzet<i>?</i></b></span>")

(defn split-paragraph [text-loc])

(def legal-tags #{"b" "i" "u" "s" "sup" "sub"})


;; TODO: br tamogatas azert jo lenne

;; ne legyen break.
(defn- parse-html [xml]
  ;; TODO: itt dobjunk kivetelt, ha nem vart HTML element is elofordul!
  ;; #"<\s*(\w+)[\s\/>]"
  (xml/parse-str (.replaceAll (str xml) "<br>" "<br/>")))

;; (prepare-html "asd<br>ad")

(defn- walk-children [xml]
  (if (map? xml)
    (if (= :br (:tag xml))
      [{:text ::br}]
      (for [c (:content xml)
            x (walk-children c)]
        (update x :path conj (:tag xml))))
    [{:text xml}]))

;; (walk-children (parse-html test-text))

(def block-tags #{:div :p})



(def prop-b {:tag :w/b :attrs {:r/val "true"}})
(def prop-i {:tag :w/i :attrs {:r/val "true"}})
(def prop-s {:tag :w/strike :attrs {:r/val "true"}})
(def prop-u {:tag :w/u :attrs {:r/val "single"}})

(def prop-sup {:tag :w/vertAlign :attrs {:r/val "superscript"}})
(def prop-sub {:tag :w/vertAlign :attrs {:r/val "subscript"}})

(defn- color [c] {:tag :w/color {:attrs {:r/val (str c)}}})

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

;; (html->ooxml test-text)
