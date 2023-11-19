(ns stencil.process
  "These functions are called from Java."
  (:import [java.io File]
           [java.util.zip ZipEntry ZipOutputStream]
           [io.github.erdos.stencil EvaluatedDocument PrepareOptions PreparedFragment PreparedTemplate TemplateVariables]
           [io.github.erdos.stencil.impl FileHelper ZipHelper LifecycleLock])
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
        model     (model/load-template-model zip-dir options)
        variables (TemplateVariables/fromPaths (get-variable-names model) (get-fragment-names model))
        datetime  (java.time.LocalDateTime/now)
        lock      (new LifecycleLock #(FileHelper/forceDelete zip-dir))]
    (reify PreparedTemplate
      (getTemplateFile [_] template-file)
      (creationDateTime [_] datetime)
      (render [_ fragments function data]
        (let [data        (into {} (.getData data))
              function    (fn [name args] (.call function name (into-array Object args)))
              fragments   (update-vals fragments datafy)
              all-locks   (cons lock (keep ::lock (vals fragments)))
              run-locked  #(LifecycleLock/execute all-locks %)
              writers-map (run-locked #(model/template-model->writers-map model data function fragments))]
          (reify EvaluatedDocument
            (write [_ target-stream]
              (run-locked #(render-writers-map writers-map target-stream))))))
      (close [_] (.close lock))
      (getVariables [_] variables)
      Object
      (toString [_] (str "<PreparedTemplate of " template-file ">"))
      Datafiable
      (datafy [_] model))))

;; Called from Java API
(defn prepare-fragment [^File fragment-file, ^PrepareOptions options]
  (let [zip-dir (FileHelper/createNonexistentTempFile
                 (.getTemporaryDirectoryOverride options)
                 "stencil-fragment-" ".zip.contents")
        options {:only-includes (.isOnlyIncludes options)}
        _       (with-open [zip-stream (io/input-stream fragment-file)]
                  (ZipHelper/unzipStreamIntoDirectory zip-stream zip-dir))
        lock    (new LifecycleLock #(FileHelper/forceDelete zip-dir))
        model   (-> (model/load-fragment-model zip-dir options)
                    (assoc ::lock lock))]
    (reify
      PreparedFragment (close [_] (.close lock))
      Object           (toString [_] (str "<PreparedFragment of " fragment-file ">"))
      Datafiable       (datafy [_] model))))

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