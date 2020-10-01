(defproject io.github.erdos/stencil-service "0.3.10-SNAPSHOT"
  :description "Web service for the Stencil templating engine"
  :url "https://github.com/erdos/stencil"
  :license {:name "Eclipse Public License - v 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.slf4j/slf4j-simple "2.0.0-alpha1"]
                 [io.github.erdos/stencil-core "0.3.9"]
                 [http-kit "2.5.0"]
                 [ring/ring-json "0.4.0"]]
  :main stencil.service.core)
