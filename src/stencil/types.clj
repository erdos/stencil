(ns stencil.types
  (:import [clojure.lang IDeref]))

(set! *warn-on-reflection* true)

(def open-tag "{%")
(def close-tag "%}")

(defrecord OpenTag [open])
(defrecord CloseTag [close])
(defrecord TextTag [text])

(defn ->text [t] (->TextTag t))
(defn ->close [t] (->CloseTag t))
(def ->open ->OpenTag)

(defprotocol ControlMarker)

;; Invocation of a fragment by name
(defrecord FragmentInvoke [result] ControlMarker)

;; Tells if a table column should be hidden in a postprocess step.
(defrecord HideTableColumnMarker [columns-resize] ControlMarker)

(def column-resize-modes #{:resize-first :resize-last :rational :cut})

(defn ->HideTableColumnMarker
  ([] (HideTableColumnMarker. :cut))
  ([x] (assert (column-resize-modes x))
       (HideTableColumnMarker. x)))

;; Tells if a table row should be hidden in a postprocess step.
(defrecord HideTableRowMarker [] ControlMarker)

(defn hide-table-column-marker? [x] (instance? HideTableColumnMarker x))
(defn hide-table-row-marker? [x] (instance? HideTableRowMarker x))

;; Tells if the reference of an adjacent image node should be replaced in postprocess step.
(defrecord ReplaceImage [relation] ControlMarker)

(defn control? [x] (satisfies? ControlMarker x))
