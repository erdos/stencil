(ns stencil.types
  (:require [clojure.pprint])
  (:gen-class))

(set! *warn-on-reflection* true)

(def open-tag "{%")
(def close-tag "%}")

(defrecord OpenTag [open])
(defmethod clojure.pprint/simple-dispatch OpenTag [t] (print (str "<" (:open t) ">")))

(defrecord CloseTag [close])
(defmethod clojure.pprint/simple-dispatch CloseTag [t] (print (str "</" (:close t) ">")))

(defrecord TextTag [text])
(defmethod clojure.pprint/simple-dispatch TextTag [t] (print (str "'" (:text t) "'")))

(defn ->text [t] (->TextTag t))
(defn ->close [t] (->CloseTag t))
(def ->open ->OpenTag)

;; A fragment represents a template in a document.
;; It can be copied and pasted to other templates. It contains all contextual
;; and meta information needed to be seamlessly merged into a document.
(defrecord Fragment [before after content])

;; Invocation of a fragment by name
(defrecord FragmentInvoke [name])

;; egyedi parancs objektumok

;; ez a marker jeloli, hogy egy oszlopot el kell rejteni.
(defrecord HideTableColumnMarker [columns-resize])

(def column-resize-modes #{:resize-last :rational :cut})

(defn ->HideTableColumnMarker
  ([] (HideTableColumnMarker. :cut))
  ([x] (assert (column-resize-modes x))
       (HideTableColumnMarker. x)))

;; ez a marker jeloli, hogy egy egesz sort el kell rejteni.
(defrecord HideTableRowMarker [])

(defn hide-table-column-marker? [x] (instance? HideTableColumnMarker x))
(defn hide-table-row-marker? [x] (instance? HideTableRowMarker x))

;; Function calls might return delayed values that are dereferenced
;; only in the postprocess stage.
(defrecord DelayedValueMarker [delay-object]
  clojure.lang.IDeref
  (deref [_] @delay-object))

(defmulti control? type)
(defmethod control? :default [_] false)
(defmethod control? HideTableColumnMarker [_] true)
(defmethod control? HideTableRowMarker [_] true)
