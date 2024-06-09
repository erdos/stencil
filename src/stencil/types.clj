(ns stencil.types)

(set! *warn-on-reflection* true)

(def open-tag "{%")
(def close-tag "%}")

(defprotocol ControlMarker)

;; Invocation of a fragment by name
(defrecord FragmentInvoke [result] ControlMarker)

(defn control? [x] (satisfies? ControlMarker x))
