(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.tools.build.util.file :as file]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))
(def javadoc-dir "target/javadoc")

(def basis (b/create-basis {:project "deps.edn"}))
(def version "0.5.10-SNAPSHOT")
(def app-name "stencil-core")
(def uber-file-name (format "%s/%s-%s-standalone.jar" build-folder app-name version)) ; path for result uber file

(defn clean [_]
  (b/delete {:path build-folder})
  (println (format "Build folder \"%s\" removed" build-folder)))


(defn compile-java [_]
  (clean nil)
  (println :should-compile-java-here)
  (b/javac {:src-dirs  ["java-src"]
            :basis basis
            :class-dir jar-content
            :javac-opts ["-source" "8" "-target" "8"]}))

(defn javadoc [opts]
  (file/ensure-dir javadoc-dir)
  (let [src-dirs ["java-src"]
        args ["-d" javadoc-dir]
        java-files (mapcat #(file/collect-files (b/resolve-path %) :collect (file/suffixes ".java")) src-dirs)
        args (into args (map str) java-files)
        tool  (javax.tools.ToolProvider/getSystemDocumentationTool)
        exit (.run tool nil nil nil (into-array String args))]
    (if (zero? exit)
      opts
      (throw (ex-info "Javadoc command error" {:exit exit})))))

(defn jar [_]
  (clean nil)
  (compile-java nil)
  (b/compile-clj {:basis     basis               ; compile clojure code
                  :src-dirs  ["src"]
                  :class-dir jar-content})
  (println "jar done?"))

(defn pom [_]
  (println "Generating pom.xml file")
  (b/write-pom
   {:basis basis
    :version version
    :lib 'io.github.erdos/stencil-core
    :target "."
    :src-pom "scripts/pom.template.xml"
    #_:pom-data
    #_[[:licenses
        [:license
         [:name "Eclipse Public License - v 2.0"]
         [:url "https://www.eclipse.org/legal/epl-2.0/"]
         [:distribution "repo"]]]]}))

(defn java-test [_]
  (def basis (b/create-basis {:project "deps.edn" :aliases [:junit]}))

  (println "Running Java test cases")
  (println "- compiling java sources")
  (b/javac {:src-dirs  ["java-src" "java-test"]
            :basis basis
            :class-dir jar-content
            :javac-opts ["-source" "8" "-target" "8"]})
  (println "- compiling clj sources")
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src"]
                  :class-dir jar-content})
  (-> {:basis basis
       :main "org.junit.platform.console.ConsoleLauncher"
       :main-args ["-p" "io.github.erdos.stencil"
                   "--fail-if-no-tests"
                   "--reports-dir=target/surefire-reports"]}
      (b/java-command)
      (b/process)
      (#(when-not (zero? (:exit %)) (throw (ex-info "junit error" %)))))
  (println "Done"))

(defn uber [_]
  (clean nil)

  ;(b/copy-dir {:src-dirs   ["resources"]         ; copy resources
  ;             :target-dir jar-content})

  (b/compile-clj {:basis     basis               ; compile clojure code
                  :src-dirs  ["src"]
                  :class-dir jar-content})

  (b/uber {:class-dir jar-content                ; create uber file
           :uber-file uber-file-name
           :basis     basis
           :main      'dev.core})                ; here we specify the entry point for uberjar

  (println (format "Uber file created: \"%s\"" uber-file-name)))

(defn test [_]
  (clean nil)
  (compile-java nil)
  ;(compile-clj nil)
; run test cases?
  )