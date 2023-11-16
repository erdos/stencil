(ns stencil.process
  "These functions are called from Java."
  (:import [java.io File]
           [java.util.zip ZipEntry ZipOutputStream]
           [io.github.erdos.stencil EvaluatedDocument PrepareOptions PreparedFragment PreparedTemplate TemplateVariables]
           [io.github.erdos.stencil.impl FileHelper ZipHelper])
  (:require [clojure.core.protocols :refer [Datafiable]]
            [clojure.datafy :refer [datafy]]
            [clojure.java.io :as io]
            [stencil.log :as log]
            [stencil.model :as model]
            [stencil.model.common :refer [unix-path]]))

(set! *warn-on-reflection* true)
(declare render-writers-map)

;; merge a set of fragment names under the :fragments key
(defn- get-fragment-names [model]
  (-> #{}
      (into (-> model :main :executable :fragments))
      (into (for [x (:headers+footers (:main model))
                  f (:fragments (:executable x))] f))))

(defn- get-variable-names [model]
  (-> #{}
      (into (-> model :main :executable :variables))
      (into (for [x (:headers+footers (:main model))
                  v (:variables (:executable x))] v))))

;; Called  from Java API
(defn prepare-template [^File template-file, ^PrepareOptions options]
  (let [zip-dir   (FileHelper/createNonexistentTempFile
                   (.getTemporaryDirectoryOverride options)
                   "stencil-" ".zip.contents")
        options   {:only-includes (.isOnlyIncludes options)}
        _         (with-open [zip-stream (io/input-stream template-file)]
                    (ZipHelper/unzipStreamIntoDirectory zip-stream zip-dir))
        model     (atom (model/load-template-model zip-dir options))
        variables (TemplateVariables/fromPaths (get-variable-names @model) (get-fragment-names @model))
        datetime  (java.time.LocalDateTime/now)]
    (reify PreparedTemplate
      (getTemplateFile [_] template-file)
      (creationDateTime [_] datetime)
      ;; TODO: rather than returning a secret objet, let's add a .render() function here.
      (render [_ fragments function data]
        ;; TODO: use lifecycle lock here
        (let [data        (into {} (.getData data))
              function    (fn [name & args] (.call function) (into-array Object args))
              writers-map (model/template-model->writers-map @model data function (update-vals fragments datafy))
              writer      (partial render-writers-map writers-map)]
          (reify EvaluatedDocument
            (write [_ target-stream]
              ;; TODO: use lifecycle lock here as well.<
              (writer target-stream)))))
      (close [_]
        (reset! model nil)
        (FileHelper/forceDelete zip-dir))
      (getVariables [_] variables))))

;; Called from Java API
(defn prepare-fragment [^File fragment-file, ^PrepareOptions options]
  (let [zip-dir (FileHelper/createNonexistentTempFile
                 (.getTemporaryDirectoryOverride options)
                 "stencil-fragment-" ".zip.contents")
        options {:only-includes (.isOnlyIncludes options)}
        _       (with-open [zip-stream (io/input-stream fragment-file)]
                  (ZipHelper/unzipStreamIntoDirectory zip-stream zip-dir))
        model   (atom (model/load-fragment-model zip-dir options))]
    (reify PreparedFragment
      (getImpl [_]
        (or @model (throw (IllegalStateException. "Fragment has alrady been cleared."))))
      (close [_]
        (reset! model nil)
        (FileHelper/forceDelete zip-dir))
      Object
      (toString [_] (str "<PreparedTemplate of " fragment-file ">"))
      clojure.core.protocols/Datafiable
      (datafy [_] @model))))

(defn- render-writers-map [writers-map outstream]
  (assert (map? writers-map))
  (io!
   (with-open [zipstream (new ZipOutputStream outstream)]
     (doseq [[k writer] writers-map
             :let  [rel-path (unix-path (io/file k))
                    ze       (new ZipEntry rel-path)]]
       (assert (not (.contains rel-path "../")))
       (log/trace "ZIP: writing {}" rel-path)
       (.putNextEntry zipstream ze)
       (writer zipstream)
       (.closeEntry zipstream)))))

;; Called from Java API
(defn eval-template [{:keys [template data function fragments]}]
  (assert (:source-folder template))
  (let [data        (into {} data)
        writers-map (model/template-model->writers-map template data function fragments)
        writer      (partial render-writers-map writers-map)]
    (reify EvaluatedDocument
      (write [_ target-stream] (writer target-stream)))))