(ns build
  (:require [clojure.tools.build.api :as b]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))

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
            :class-dir "target/classes"
            :javac-opts ["-source" "8" "-target" "8"]}))

(defn jar [_]
  (clean nil)
  (compile-java nil)
  (b/compile-clj {:basis     basis               ; compile clojure code
                  :src-dirs  ["src"]
                  :class-dir jar-content})
  (println "jar done?"))

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