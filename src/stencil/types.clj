(ns stencil.types)

(set! *warn-on-reflection* true)

(def open-tag "{%")
(def close-tag "%}")

(defprotocol ControlMarker)

;; Invocation of a fragment by name
(defrecord FragmentInvoke [result] ControlMarker)

;; Tells if a table column should be hidden in a postprocess step.
(defrecord HideTableColumnMarker [columns-resize] ControlMarker)

(def column-resize-modes #{:resize-first :resize-last :rational :cut})

#_{:clj-kondo/ignore [:redefined-var]}
(defn ->HideTableColumnMarker
  ([] (HideTableColumnMarker. :cut))
  ([x] (assert (column-resize-modes x))
       (HideTableColumnMarker. x)))

;; Tells if a table row should be hidden in a postprocess step.
(defrecord HideTableRowMarker [] ControlMarker)

(defn hide-table-column-marker? [x] (instance? HideTableColumnMarker x))
(defn hide-table-row-marker? [x] (instance? HideTableRowMarker x))

(defn control? [x] (satisfies? ControlMarker x))
