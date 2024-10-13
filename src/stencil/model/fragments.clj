(ns stencil.model.fragments
  (:require [stencil.util :refer [eval-exception]]
            [stencil.functions :refer [def-stencil-fn]]
            [stencil.types :refer [ControlMarker]]
            [stencil.ooxml :as ooxml]
            [clojure.data.xml :as xml]))

;; all insertable fragments. map of id to frag def.
(def ^:dynamic *all-fragments* nil)

;; set of already inserted fragment ids.
(def ^:dynamic *inserted-fragments* nil)

(defmacro with-fragments [fragments body]
  `(binding [*inserted-fragments* (atom #{})
             *all-fragments*      (into {} ~fragments)]
     ~body))

(defn use-fragment [frag-name]
  (if-let [fragment (get *all-fragments* frag-name)]
    (do (swap! *inserted-fragments* conj frag-name)
        fragment)
    (throw (eval-exception (str "No fragment for name: " frag-name) nil))))

;; evaluates body in a clear fragment context and returns a tuple of [result newly-added-fragments]
(defmacro with-sub-fragments [body]
  `(let [[result# fragments#] (binding [*inserted-fragments* (atom #{})]
                                [~body @*inserted-fragments*])]
     (swap! *inserted-fragments* into fragments#)
     [result# fragments#]))

;; Invocation of a fragment by name
(defrecord FragmentInvoke [result] ControlMarker)

;; custom XML content
(def-stencil-fn "xml"
  "Inserts OOXML fragment into the document from the parameter of this function call.
   Usage: `{%=xml(ooxml)%}`"
  [content]
  (assert (string? content))
  (let [content (:content (xml/parse-str (str "<a>" content "</a>")))]
    (->FragmentInvoke {:frag-evaled-parts content})))

;; inserts a page break at the current run.
(let [br {:tag ooxml/br :attrs {ooxml/type "page"}}
      page-break (->FragmentInvoke {:frag-evaled-parts [br]})]
  (def-stencil-fn "pageBreak"
    "Inserts page break into the document where the return value of this function is used.
     Usage: `{%=pageBreak()%}`"
    [] page-break))
