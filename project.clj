(defproject io.github.erdos/stencil-core "0.3.22-SNAPSHOT"
  :url "https://github.com/erdos/stencil"
  :description       "Templating engine for office documents."
  :license {:name "Eclipse Public License - v 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :min-lein-version  "2.0.0"
  :java-source-paths ["java-src"]
  :javac-options     ["-target" "8" "-source" "8"]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.xml "0.2.0-alpha6"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.slf4j/slf4j-api "1.6.2"]]
  :pom-addition ([:properties ["maven.compiler.source" "8"] ["maven.compiler.target" "8"]])
  :pom-plugins [[org.apache.maven.plugins/maven-surefire-plugin "2.20"]]
  :main io.github.erdos.stencil.Main
  :aliases      {"junit" ["with-profile" "+test" "test-out" "junit" "junit.xml"]
                 "coverage" ["with-profile" "+ci" "cloverage" "--codecov" "--exclude-call" "clojure.core/assert" "--exclude-call" "stencil.util/trace" "--exclude-call" "stencil.util/fail"]}
  :javadoc-opts {:package-names ["stencil"]
                 :additional-args ["-overview" "java-src/overview.html"
                                   "-top" "<style>kbd{background:#ddd}; a[title~=class], a[title~=interface], a[title~=enum]{text-decoration: underline; font-weight: bold} dd>code{background:#eee}</style>"]}
  :repl-options {:init-ns stencil.api}
  :jar-exclusions [#".*\.xml"]
  :repositories [["snapshots" {:url "https://clojars.org/repo"
                               :username :env/clojars_user
                               :password :env/clojars_pass
                               :sign-releases false}]]
  :profiles {:uberjar {:aot :all}
             :dev {:aot :all
                   :dependencies [[org.slf4j/slf4j-simple "1.6.2"]]
                   :injections [(require 'stencil.spec)
                                (require '[clojure.spec.alpha :as s])]
                   :jvm-opts ["-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"]}
             :test {:aot :all
                    :dependencies [[junit/junit "4.12"]
                                   [org.xmlunit/xmlunit-core "2.5.1"]
                                   [hiccup "1.0.5"]]
                    :plugins      [[lein-test-out "0.3.1"]]
                    :resource-paths    ["test-resources"]
                    :test-paths ["java-test"]
                    :injections [(require 'stencil.spec)
                                 (require '[clojure.spec.test.alpha :as sta])
                                 (eval '(run! sta/instrument (clojure.spec.test.alpha/instrumentable-sys)))]}
             :ci {:plugins [[lein-javadoc "0.3.0"]
                            [lein-cloverage "1.2.2"]]
                  }})