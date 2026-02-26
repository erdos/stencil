(defproject io.github.erdos/stencil-service "0.6.7-SNAPSHOT"
  :description "Web service for the Stencil templating engine"
  :url "https://github.com/erdos/stencil"
  :license {:name "Eclipse Public License - v 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [io.github.erdos/stencil-core "0.6.7-SNAPSHOT"]
                 [org.slf4j/slf4j-api "2.0.9"]
                 [org.mozilla/rhino-engine "1.7.14"]
                 [http-kit "2.7.0"]
                 [ring/ring-json "0.5.0"]]
  :aot :all
  :main stencil.service)
