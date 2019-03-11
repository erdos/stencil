(defproject io.github.erdos/stencil-service "0.2.9"
  :description "Web service for the Stencil templating engine"
  :url "https://github.com/erdos/stencil"
  :license {:name "Eclipse Public License - v 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [io.github.erdos/stencil-core "0.2.9"]
                 [http-kit "2.2.0"]
                 [ring/ring-json "0.4.0"]]
  :main stencil.service.core)
