(ns stencil.model.fragments
  (:require [stencil.util :refer [eval-exception]]
            [stencil.functions :refer [call-fn]]
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
(defrecord FragmentInvoke [result])

;; custom XML content
(defmethod call-fn "xml" [_ content]
  (assert (string? content))
  (let [content (:content (xml/parse-str (str "<a>" content "</a>")))]
    (->FragmentInvoke {:frag-evaled-parts content})))

;; inserts a page break at the current run.
(let [br {:tag ooxml/br :attrs {ooxml/type "page"}}
      page-break (->FragmentInvoke {:frag-evaled-parts [br]})]
  (defmethod call-fn "pageBreak" [_] page-break))
