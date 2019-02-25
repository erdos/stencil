(defproject io.github.erdos/stencil-serverless "0.2.8"
  :description "Serverless application for the Stencil templating engine"
  :url "https://github.com/erdos/stencil"
  :license {:name "Eclipse Public License - v 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 #_ [io.github.erdos/stencil-core "0.2.8"]]
  :aot :all
  :main stencil.serverless.core)
