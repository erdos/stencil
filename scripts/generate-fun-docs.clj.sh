#!/usr/bin/env sh
test ; # file is both a valid SH and a valid CLJ file at the same time.

test ; set -e && cd "$(dirname "$0")/.." && clojure -M -i scripts/generate-fun-docs.clj.sh > target/Functions.md && exit 0

;; file is a regular CLJ script from now on

(require 'stencil.functions)

(defn get-java-functions []
  (for [f (.listFunctions (new io.github.erdos.stencil.functions.FunctionEvaluator))]
    {:name (.getName f)
     :docs (.getDocumentation f)}))

(defn get-clj-functions []
  (for [[k v] (methods stencil.functions/call-fn)]
    {:name k
     :docs (:stencil.functions/docs (meta v))}))

(def all-functions
  (sort-by :name (concat (get-java-functions) (get-clj-functions))))

(println "# Functions")
(println)
(println "You can call functions from within the template files and embed the call result easily by writing `{%=functionName(arg1, arg2, arg3, ...)%}` expression in the document template.")
(println "This page contains a short description of the functions implemented in Stencil.")
(println)

;; Table of Contents
(doseq [f all-functions]
  (printf "- [%s](#%s)\n" (:name f) (:name f)))

(println)

(doseq [f all-functions]
  (printf "## %s\n\n" (:name f))
  (println (:docs f))
  (println))
