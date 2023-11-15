(ns stencil.process
  "These functions are called from Java."
  (:import [java.io File InputStream]
           [java.util.zip ZipEntry ZipOutputStream]
           [io.github.erdos.stencil PrepareOptions PreparedTemplate TemplateVariables]
           [io.github.erdos.stencil.impl FileHelper ZipHelper])
  (:require [clojure.java.io :as io]
            [stencil.log :as log]
            [stencil.model :as model]))

(set! *warn-on-reflection* true)

;; merge a set of fragment names under the :fragments key
(defn- merge-fragment-names [model]
  (assoc model
         :fragments
         (-> #{}
             (into (-> model :main :executable :fragments))
             (into (for [x (:headers+footers (:main model))
                         f (:fragments (:executable x))] f)))))

(defn- merge-variable-names [model]
  (assoc model
         :variables
         (-> #{}
             (into (-> model :main :executable :variables))
             (into (for [x (:headers+footers (:main model))
                         v (:variables (:executable x))] v)))))

;; Called  from Java API
(defn prepare-template [^File template-file, ^PrepareOptions options]
  (let [zip-dir   (FileHelper/createNonexistentTempFile
                   (.getTemporaryDirectoryOverride options)
                   "stencil-" ".zip.contents")
        options   {:only-includes (.isOnlyIncludes options)}
        _         (with-open [zip-stream (io/input-stream template-file)]
                    (ZipHelper/unzipStreamIntoDirectory zip-stream zip-dir))
        model (-> (model/load-template-model zip-dir options)
                  (merge-fragment-names)
                  (merge-variable-names)
                  (atom))
        datetime (java.time.LocalDateTime/now)
        variables (TemplateVariables/fromPaths (:variables @model) (:fragments @model))]
    ;; (FileHelper/forceDeleteOnExit zip-dir)
    (reify PreparedTemplate
      (getTemplateFile [_] template-file)
      (creationDateTime [_] datetime)
      (getSecretObject [_]
        (or @model (throw (IllegalStateException. "Template has already been cleared."))))
      (close [_]
        (reset! model nil)
        (FileHelper/forceDelete zip-dir))
      (getVariables [_] variables))))

;; Called from Java API
(defn prepare-fragment [input, ^PrepareOptions options]
  (assert (some? input))
  (let [zip-dir (FileHelper/createNonexistentTempFile
                 (.getTemporaryDirectoryOverride options)
                 "stencil-fragment-" ".zip.contents")
        options {:only-includes (.isOnlyIncludes options)}]
    (with-open [zip-stream (io/input-stream input)]
      (ZipHelper/unzipStreamIntoDirectory zip-stream zip-dir))
    (model/load-fragment-model zip-dir options)))

(defn- render-writers-map [writers-map outstream]
  (assert (map? writers-map))
  (io!
   (with-open [zipstream (new ZipOutputStream outstream)]
     (doseq [[k writer] writers-map
             :let  [rel-path (FileHelper/toUnixSeparatedString (.toPath (io/file k)))
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
        writers-map (model/template-model->writers-map template data function fragments)]
    {:writer (partial render-writers-map writers-map)}))