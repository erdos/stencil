(ns stencil.process
  "A konvertalas folyamat osszefogasa"
  (:gen-class)
  (:import [java.io File PipedInputStream PipedOutputStream InputStream]
           [java.util.zip ZipEntry ZipOutputStream]
           [io.github.erdos.stencil.impl FileHelper ZipHelper])
  (:require [clojure.data.xml :as xml]
            [clojure.data.xml.pu-map :as pu-map]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [stencil.postprocess.ignored-tag :as ignored-tag]
            [stencil
             [tokenizer :as tokenizer]
             [cleanup :as cleanup]
             [eval :as eval]
             [tree-postprocess :as tree-postprocess]]))

(set! *warn-on-reflection* true)

(defn- ->executable [readable]
  (with-open [r (io/reader readable)]
    (cleanup/process (tokenizer/parse-to-tokens-seq r))))

(defmulti prepare-template
  ;; extension: template file name extension
  ;; stream: template file contents
  (fn [extension stream] (some-> extension name .trim .toLowerCase keyword)))

(defmethod prepare-template :default [ext _]
  (throw (ex-info (format "Unrecognized extension: '%s'" ext) {:extension ext})))

(defmethod prepare-template :xml [_ stream]
  (let [m (->executable stream)]
    {:variables  (:variables m)
     :type       :xml
     :executable (:executable m)
     :fragments  (:fragments m)}))

(defn- prepare-zipped-xml-files [suffix ^InputStream stream]
  (assert (some? suffix))
  (assert (instance? InputStream stream))
  (let [zip-dir   (FileHelper/createNonexistentTempFile "stencil-" (str suffix ".zip.contents"))]
    (with-open [zip-stream stream] ;; FIXME: maybe not deleted immediately
      (ZipHelper/unzipStreamIntoDirectory zip-stream zip-dir))
    (let [files (fn [dir] (for [w (.list (io/file zip-dir dir))
                               :when (.endsWith (.toLowerCase (str w)) ".xml")]
                           (FileHelper/toUnixSeparatedString (.toPath (io/file dir w)))))
          xml-files (concat (files "word") (files (io/file "ppt" (io/file "slides"))))
          execs     (zipmap xml-files (map #(->executable (io/file zip-dir %)) xml-files))]
      ;; TODO: maybe make it smarter by loading only important xml files
      ;; such as document.xml and footers/headers
      {:zip-dir    zip-dir
       :type       :docx
       :variables  (set (mapcat :variables (vals execs)))
       :fragments  (into {} (keep :fragments (vals execs)))
       :exec-files (into {} (for [[k v] execs
                                  :when (:dynamic? v)]
                              [k (:executable v)]))})))

(defmethod  prepare-template :docx [suffix stream] (prepare-zipped-xml-files suffix stream))
(defmethod  prepare-template :pptx [suffix stream] (prepare-zipped-xml-files suffix stream))

(defn- run-executable-and-return-writer
  "Returns a function that writes output to its output-stream parameter"
  [executable function data]
  (let [result (-> (eval/normal-control-ast->evaled-seq data function executable)
                   (tokenizer/tokens-seq->document)
                   (tree-postprocess/postprocess)
                   (ignored-tag/unmap-ignored-attr))]
    (fn [output-stream]
      (let [writer (io/writer output-stream)]
        (xml/emit result writer)
        (.flush writer)))))

(defmulti do-eval-stream (comp :type :template))

(defn- handle-zipped-xml-files [{:keys [template data function]}]
  (assert (:zip-dir template))
  (assert (:exec-files template))
  (let [data   (into {} data)
        {:keys [zip-dir exec-files]} template
        source-dir   (io/file zip-dir)
        source-dir-path (.toPath source-dir)
        outstream    (new PipedOutputStream)
        input-stream (new PipedInputStream outstream)
        executed-files (into {}
                             (for [[rel-path executable] exec-files]
                               [rel-path (run-executable-and-return-writer executable function data)]))]
    (future
      (try
        (with-open [zipstream (new ZipOutputStream outstream)]
          (doseq [file  (file-seq source-dir)
                  :when (not      (.isDirectory ^File file))
                  :let  [path     (.toPath ^File file)
                         rel-path (FileHelper/toUnixSeparatedString (.relativize source-dir-path path))
                         ze       (new ZipEntry rel-path)]]
            (.putNextEntry zipstream ze)
            (if-let [writer (get executed-files rel-path)]
              (writer zipstream)
              (java.nio.file.Files/copy path zipstream))
            (.closeEntry zipstream)))
        (catch Throwable e
          (println "Zipping exception: " e))))
    {:stream input-stream
     :format :docx}))

(defmethod do-eval-stream :docx [template] (handle-zipped-xml-files template))

(defmethod do-eval-stream :pptx [template] (handle-zipped-xml-files template))

(defmethod do-eval-stream :xml [{:keys [template data function] :as input}]
  (assert (:executable template))
  (let [data         (into {} data)
        executable   (:executable template)
        out-stream   (new PipedOutputStream)
        input-stream (new PipedInputStream out-stream)
        writer (run-executable-and-return-writer executable function data)]
    (future
      ;; TODO: itt hogyan kezeljunk hibat?
      (try
        (with-open [out-stream out-stream]
          (writer out-stream))
        (catch Throwable e
          (println "Evaling exception: " e))))
    {:stream input-stream
     :format :xml}))
