(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.tools.build.util.file :as file]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))
(def javadoc-dir "target/javadoc")

(def basis (b/create-basis {:project "deps.edn"}))

(def version (-> basis :aliases :stencil/version (doto assert)))

(def lib 'io.github.erdos/stencil-core)

(def jar-file-name (format "%s/%s-%s.jar" build-folder (name lib) version))
(def uber-file-name (format "%s/%s-%s-standalone.jar" build-folder (name lib) version))

(defn clean [opts]
  (b/delete {:path build-folder})
  (println (format "Build folder \"%s\" removed" build-folder))
  opts)

(defn compile-java [opts]
  (clean opts)
  (println :should-compile-java-here)
  (b/javac {:src-dirs  ["java-src"]
            :basis basis
            :class-dir jar-content
            :javac-opts ["-source" "8" "-target" "8"]})
  (b/copy-file {:src "java-src/io/github/erdos/stencil/standalone/help.txt"
                :target "target/classes/io/github/erdos/stencil/standalone/help.txt"})
  (spit (str jar-content "/stencil-version") version)
  opts)

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

(defn pom [opts]
  (println "Generating pom.xml file")
  (b/write-pom
   {:class-dir jar-content
    :basis basis
    :version version
    :lib lib
    :pom-data
    [[:licenses
      [:license
       [:name "Eclipse Public License - v 2.0"]
       [:url "https://www.eclipse.org/legal/epl-2.0/"]
       [:distribution "repo"]]]]})
  opts)

(defn jar [opts]
  (clean opts)
  (compile-java opts)
  (pom opts)
  (b/copy-dir {:src-dirs ["src"] :target-dir jar-content})
  (b/jar      {:class-dir jar-content
               :jar-file jar-file-name})
  (println "Built JAR file")
  opts)

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
                  :class-dir jar-content
                  :bindings {#'*warn-on-reflection* true}})
  (-> {:basis basis
       :main "org.junit.platform.console.ConsoleLauncher"
       :main-args ["-p" "io.github.erdos.stencil"
                   "--fail-if-no-tests"
                   "--reports-dir=target/surefire-reports"]}
      (b/java-command)
      (b/process)
      (#(when-not (zero? (:exit %)) (throw (ex-info "junit error" %)))))
  (println "Done"))

(defn uber [opts]
  (jar opts) 
  (b/uber {:class-dir jar-content
           :uber-file uber-file-name
           :basis     basis
           :main      'io.github.erdos.stencil.Main})
  (println (format "Uber file created: \"%s\"" uber-file-name))
  opts)

(defn install [opts]
  (jar opts)
  (b/install {:basis basis
              :lib lib
              :version version
              :class-dir jar-content
              :jar-file jar-file-name})
  opts)
