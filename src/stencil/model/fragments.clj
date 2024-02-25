(ns stencil.model.fragments
  (:require [stencil.util :refer [eval-exception]]))

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

;; evaluates body in a clear fragment context and returns a tuple of [newly-added-fragments result]
(defmacro with-sub-fragments [body]
  `(let [[result# fragments#] (binding [*inserted-fragments* (atom #{})]
                                [~body @*inserted-fragments*])]
     (swap! *inserted-fragments* into fragments#)
     [result# fragments#]))
