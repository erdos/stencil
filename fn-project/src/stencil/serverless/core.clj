(ns stencil.serverless.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]))

(defn -main [& args]
  (println (read-line)))
