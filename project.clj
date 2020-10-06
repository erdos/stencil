(defproject io.github.erdos/stencil-core "0.3.10-SNAPSHOT"
  :url "https://github.com/erdos/stencil"
  :description       "Templating engine for office documents."
  :license {:name "Eclipse Public License - v 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :min-lein-version  "2.0.0"
  :java-source-paths ["java-src"]
  :javac-options     ["-target" "8" "-source" "8"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.xml "0.2.0-alpha5"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.slf4j/slf4j-api "1.8.0-beta2"]]
  :pom-addition ([:properties ["maven.compiler.source" "8"] ["maven.compiler.target" "8"]])
  :pom-plugins [[org.apache.maven.plugins/maven-surefire-plugin "2.20"]]
  :main io.github.erdos.stencil.Main
  :plugins [[lein-javadoc "0.3.0"]
            [lein-cloverage "1.1.1"]
            [lein-test-out "0.3.1"]]
  :aliases      {"junit" ["with-profile" "test" "do" "test-out" "junit" "junit.xml"]
                 "coverage" ["cloverage" "--codecov" "--exclude-call" "clojure.core/assert" "--exclude-call" "stencil.util/trace" "--exclude-call" "stencil.util/fail"]}
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
             :dev {:aot :all}
             :test {:aot :all
                    :dependencies [[junit/junit "4.12"]
                                   [org.xmlunit/xmlunit-core "2.5.1"]
                                   [hiccup "1.0.5"]]
                    :resource-paths    ["test-resources"]
                    :test-paths ["java-test"]
                    :java-source-paths ["java-src"]}})
