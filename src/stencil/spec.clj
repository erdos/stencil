(ns stencil.spec
  (:import [java.io File])
  (:require [clojure.spec.alpha :as s]
            [stencil.model :as m]
            [stencil.process]))


;; other types are also possible
(s/def ::m/type #{m/rel-type-footer m/rel-type-header m/rel-type-main m/rel-type-slide})

(s/def ::m/path (s/and string? not-empty #(not (.startsWith ^String % "/"))))

;; relationship file
(s/def ::relations (s/keys :req    [::m/path]
                           :req-un [::source-file]
                           :opt-un [::m/parsed]))

(s/def ::m/headers+footers (s/* (s/keys :req [::m/path]
                                        :req-un [::source-file ::m/executable ::relations])))

(s/def ::source-folder (s/and (partial instance? java.io.File)
                              #(.isDirectory ^File %)
                              #(.exists ^File %)))

(s/def ::source-file (s/and (partial instance? java.io.File)
                            #(.isFile ^File %)
                            #(.exists ^File %)))

(s/def ::main (s/keys :req [::m/path] :req-un [::numbering]))


(s/def ::m/content-types
  (s/keys :req [::m/path]
          :req-un [::source-file]))

(s/def ::m/model
  (s/keys :req []
          :req-un [::main ::source-folder ::m/content-types ::relations]))

(s/def ::parsed any?)

(s/def ::numbering (s/nilable (s/keys :req [::m/path]
                                      :req-un [::source-file ::parsed])))

(s/fdef m/load-template-model
  :args (s/cat :dir ::source-folder, :opts map?)
  :ret ::m/model)

(s/fdef m/eval-template-model
  :args (s/cat :model ::m/model, :data map?, :functions any?, :fragments any?)
  :ret  ::m/model)

(s/def :exec/variables (s/coll-of string? :unique true))
(s/def :exec/dynamic? boolean?)
(s/def :exec/executable any?) ;; seq of normalized control ast.
(s/def :exec/fragments any?) ;; what was that?

(s/def ::executable (s/keys :req-un [:exec/variables :exec/dynamic? :exec/executable :exec/fragments]))

;; prepared template
(s/def ::template any?)

(s/def :eval-template/data any?) ;; map of string or keyword keys
(s/def :eval-template/fragments (s/map-of string? any?))

(s/def :eval-template/function fn?)

(s/fdef stencil.process/eval-template
  :args (s/cat :ps (s/keys :req-un [::template :eval-template/data :eval-template/function :eval-template/fragments])))
