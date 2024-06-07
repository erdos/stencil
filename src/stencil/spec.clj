(ns stencil.spec
  (:require [clojure.spec.alpha :as s]
            [stencil.fs :as fs]
            [stencil.model :as m]
            [stencil.model.relations :as relations]
            [stencil.process]))


;; TODO
(s/def :stencil.model/mode #{"External"})

;; keys are  either all keywords or all strings
(s/def ::data map?)

;; other types are also possible
(s/def :stencil.model/type #{relations/rel-type-footer
                             relations/rel-type-header
                             relations/rel-type-main
                             relations/rel-type-slide})

(s/def :stencil.model/path (s/and string?
                                  not-empty
                                  #(not (.startsWith (str %) "/"))
                                  #(not (.endsWith (str %) "/"))
                                  #(not (.contains (str %) ".."))))

;; relationship file
(s/def ::relations (s/keys :req    [:stencil.model/path]
                           :req-un [::source-file]
                           :opt-un [:stencil.model/parsed]))

(s/def :?/relations (s/nilable ::relations))

(s/def ::result (s/keys :req-un [::writer]))

(s/def ::style (s/keys :req [:stencil.model/path]
                       :opt-un [::result]))

(s/def :stencil.model/headers+footers
  (s/* (s/keys :req [:stencil.model/path]
               :req-un [::source-file :stencil.model/executable :?/relations]
               :opt-un [::result])))

(s/def ::source-folder (s/and fs/directory? fs/exists?))

(s/def ::source-file (s/and (partial instance? java.io.File)
                            (complement fs/directory?)
                            fs/exists?))

(s/def ::main (s/keys :req [:stencil.model/path]
                      :opt-un [:stencil.model/headers+footers ::result] ;; not present in fragments
                      :opt [::numbering]
                      :req-un [::source-file
                               ::executable
                               ::style
                               ::relations]))


(s/def :stencil.model/content-types
  (s/keys :req [:stencil.model/path]
          :opt-un [::source-file]))

(s/def :stencil.model/model
  (s/keys :req []
          :req-un [::main ::source-folder :stencil.model/content-types ::relations]))

(s/def ::parsed any?)

(s/def :stencil.model/numbering
  (s/nilable (s/keys :req [:stencil.model/path]
                     :req-un [::source-file ::parsed])))

(s/fdef stencil.model/load-template-model
  :args (s/cat :dir ::source-folder, :opts map?)
  :ret :stencil.model/model)

(s/fdef stencil.model/load-fragment-model
  :args (s/cat :dir ::source-folder, :opts map?)
  :ret :stencil.model/model)

(s/fdef stencil.model/eval-template-model
  :args (s/cat :model :stencil.model/model
               :data ::data
               :unused/function-arg any?
               :fragments (s/map-of string? :stencil.model/model))
  :ret  :stencil.model/model)

(s/def :exec/variables (s/coll-of string? :unique true))
(s/def :exec/dynamic? boolean?)
(s/def :exec/executable any?) ;; seq of normalized control ast.
(s/def :exec/fragments (s/coll-of string? :unique true)) ;; what was that?

(s/def ::executable (s/keys :req-un [:exec/variables :exec/dynamic? :exec/executable :exec/fragments]))

;; prepared template
(s/def ::template any?)

(s/def :eval-template/data any?) ;; map of string or keyword keys
(s/def :eval-template/fragments (s/map-of string? any?))
(s/def :eval-template/function fn?)

(s/fdef stencil.process/eval-template
  :args (s/cat :ps (s/keys :req-un [::template :eval-template/data :eval-template/function :eval-template/fragments])))

(s/def ::writer (s/fspec :args (s/cat :writer (partial instance? java.io.Writer)) :ret nil?))

(s/fdef template-model->writers-map
  :args (s/cat :model :stencil.model/model, :data ::data, :functions any?, :fragments any?)
  :ret (s/map-of :stencil.model/path ::writer))