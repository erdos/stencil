(ns stencil.model-test
  (:require [stencil.model :refer :all]
            [stencil.api :as api]
            [clojure.java.io :refer [file resource]]
            [clojure.test :refer [deftest is are testing]]))


(let [target (the-ns 'stencil.model)]
  (doseq [[k v] (ns-map target)
          :when (and (var? v) (= target (.ns ^clojure.lang.Var v)))]
    (eval `(defn ~(symbol (str "-" k)) [~'& args#] (apply (deref ~v) args#)))))


(deftest test-load-template-model
  (let [model (.getSecretObject (api/prepare "test-resources/multipart/main.docx"))]

    (is (contains? model :main))
    (is (contains? model :content-types))

    (testing "Both header and footer are parsed"
      (is (= 2 (count (:headers+footers (:main model))))))

    (testing "All :source-file values must point to existing files"
      (doseq [item (tree-seq coll? seq model)
              :when (map? item)
              :when (not (sorted? item))
              :when (contains? item :source-file)]
        (is (instance? java.io.File (:source-file item)))
        (is (.exists ^java.io.File (:source-file item)))
        (is (.isFile ^java.io.File (:source-file item)))))

    ))

(defn- debug-model [model]
  (-> model
      (assoc-in [:content-types] :CT)
      (assoc-in [:rels] :RELS)
      (assoc-in [:main :relations :parsed] :RELS)
      (assoc-in [:main :executable :executable] :EXEC)
      (assoc-in [:main :executable :variables] :EXEC/vars)
      (update-in [:main :headers+footers] (partial mapv #(assoc-in % [:executable :executable] :EXEC)))
      (update-in [:main :headers+footers] (partial mapv #(assoc-in % [:relations :parsed] :PARSED/RELS)))))
