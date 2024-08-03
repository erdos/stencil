#!/usr/bin/env sh
test ; # file is both a valid SH and a valid CLJ file at the same time.

test ; set -e && cd "$(dirname "$0")/.." && clojure -M -i scripts/generate-fun-docs.clj.sh > target/Functions.md && exit 0

;; file is a regular CLJ script from now on

(println "HALI")

(require 'stencil.functions)

(println "# Functions")
(println)
(println "You can call functions from within the template files and embed the call result easily by writing `{%=functionName(arg1, arg2, arg3, ...)%}` expression in the document template.")
(println "This page contains a short description of the functions implemented in Stencil.")
(println)

(doseq [k (sort (keys (methods stencil.functions/call-fn)))]
  (printf "- [%s](#%s)\n" k k))

(println)

(doseq [[k f] (sort (methods stencil.functions/call-fn))]
  (printf "## %s\n\n" k)
  (when-let [docs (:stencil.functions/docs (meta f))]
    (println docs)
    (println))
  (println))
