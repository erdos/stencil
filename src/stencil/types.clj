(ns stencil.types)

(set! *warn-on-reflection* true)

(def open-tag "{%")
(def close-tag "%}")

(defprotocol ControlMarker)

(defn control? [x] (satisfies? ControlMarker x))
