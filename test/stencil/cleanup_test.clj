(ns stencil.cleanup-test
  (:require [clojure.test :refer [deftest testing is are]]
            [stencil
             [cleanup :refer :all]]))

(defn- ->text [t] {:text t})
(defn- ->close [t] {:close t})
(defn- ->open [t] {:open t})

(deftest stack-revert-close-test
  (testing "Egyszeru es ures esetek"
    (is (= [] (stack-revert-close [])))
    (is (= [] (stack-revert-close nil)))
    (is (= [] (stack-revert-close [{:whatever 1} {:cmd/else 2}]))))
  (testing "A kinyito elemeket be kell csukni"
    (is (= [(->close "a") (->close "b")]
           (stack-revert-close [{:open "b"} {:open "a"}])))))

(deftest tokens->ast-test
  (testing "Egyszeru esetek"
    (are [input output] (= output (tokens->ast input))
      [] []

      ;; nem valtozott
      [{:open 1} {:close 1}]
      [{:open 1} {:close 1}])))

(deftest tokens->ast-if-test
  (testing "felteteles elagazasra egyszeru tesztesetek"
    (are [input output] (= output (tokens->ast input))
      ;; if-then-fi
      [{:cmd       :cmd/if :condition 1}
       {:text      "Then"}
       {:cmd       :cmd/end}]
      [{:cmd :cmd/if :condition 1
        :stencil.cleanup/blocks [{:stencil.cleanup/children [{:text "Then"}]}]}]

      ;; if-then-else-fi
      [{:cmd :cmd/if :condition 1}
       {:text "Then"}
       {:cmd :cmd/else}
       {:text "Else"}
       {:cmd :cmd/end}]
      [{:cmd :cmd/if, :condition 1
        :stencil.cleanup/blocks [{:stencil.cleanup/children [{:text "Then"}]} {:stencil.cleanup/children [{:text "Else"}]}]}])))

(deftest normal-ast-test-1
  (is (= (map control-ast-normalize
          (annotate-environments
           [(->open "html")
            (->open "a")
            (->text "Inka")
            {:cmd :cmd/if
             :stencil.cleanup/blocks [{:stencil.cleanup/children [
                                  (->text "ikarusz")
                                  (->close "a")
                                  (->text "bela")
                                  (->open "b")
                                  (->text "Hello")]}
                      {:stencil.cleanup/children [
                                  (->text "Virag")
                                  (->close "b")
                                  (->text "Hajdiho!")
                                  (->open "c")
                                  (->text "Bogar")]}]}
            (->text "Kaktusz")
            (->close "c")
            (->close "html")]))

         [(->open "html")
          (->open "a")
          (->text "Inka")
          {:cmd :cmd/if
           :branch/then [(->text "ikarusz")
                  (->close "a")
                  (->text "bela")
                  (->open "b")
                  (->text "Hello")
                  (->close "b")
                  (->open "c")]
           :branch/else [(->close "a")
                  (->open "b")
                  (->text "Virag")
                  (->close "b")
                  (->text "Hajdiho!")
                  (->open "c")
                  (->text "Bogar")]}
          (->text "Kaktusz")
          (->close "c")
          (->close "html")])))

(def <i> (->open "i")) (def <／i> (->close "i"))
(def <b> (->open "b")) (def <／b> (->close "b"))
(def <j> (->open "j")) (def <／j> (->close "j"))
(def <a> (->open "a")) (def <／a> (->close "a"))

(deftest normal-ast-test-0
  (testing "Amikor a formazas a THEN blokk kozepeig tart, akkor az ELSE blokk-ba is be kell tenni a lezaro taget."
    (is (= (map control-ast-normalize
            (annotate-environments
             [{:cmd :cmd/if
               :stencil.cleanup/blocks [{:stencil.cleanup/children [(->text "bela") <b> (->text "Hello")]}
                        {:stencil.cleanup/children [(->text "Virag")]}]}
              (->close "b")]))

           [{:cmd :cmd/if
             :branch/then [(->text "bela") <b> (->text "Hello")]
             :branch/else [<b> (->text "Virag")]}
            (->close "b")]))))

(deftest normal-ast-test-0-deep
  (testing "Amikor a formazas a THEN blokk kozepeig tart, akkor az ELSE blokk-ba is be kell tenni a lezaro taget."
    (is (= (map control-ast-normalize
            (annotate-environments
             [{:cmd :cmd/if
               :stencil.cleanup/blocks [{:stencil.cleanup/children [(->text "bela") <b> <i> (->text "Hello") <／i>]}
                        {:stencil.cleanup/children [<j> (->text "Virag") <／j>]}]}
              <／b>]))

           [{:cmd :cmd/if
             :branch/then [(->text "bela") <b> <i> (->text "Hello") <／i>]
             :branch/else [<b> <j> (->text "Virag") <／j>]}
            <／b>]))))

(deftest normal-ast-test-condition-only-then
  (testing "Az elagazasban eredeetileg csak THEN ag volt de beszurjuk az else agat is."
    (is (= (map control-ast-normalize
            (annotate-environments
             [<a>
              {:cmd :cmd/if
               :stencil.cleanup/blocks [{:stencil.cleanup/children [(->text "bela") <b> <i> (->text "Hello") <／i>]}]}
              <／b>
              <／a>]))

           [<a> {:cmd :cmd/if
                 :branch/then [(->text "bela") <b> <i> (->text "Hello") <／i>]
                 :branch/else [<b>]}
            <／b>
            <／a>]))))

(defn >>for-loop [& children] {:cmd :cmd/for :stencil.cleanup/blocks [{:stencil.cleanup/children (vec children)}]})

(deftest test-normal-ast-for-loop-1
  (testing "ismetleses ciklusban"
    (is (= (map control-ast-normalize
            (annotate-environments
             [<a>
              (->text "before")
              (>>for-loop (->text "inside1") <／a> <b> (->text "inside2"))
              (->text "after")
              <／b>]))
           [<a>
            (->text "before")
            {:cmd :cmd/for
             :branch/body-run-none [<／a> <b>]
             :branch/body-run-once [(->text "inside1") <／a> <b> (->text "inside2")]
             :branch/body-run-next [<／b> <a> (->text "inside1") <／a> <b> (->text "inside2")]}
            (->text "after")
            <／b>]))))

(deftest test-find-variables
  (testing "Empty test cases"
    (is (= () (find-variables [])))
    (is (= () (find-variables nil)))
    (is (= () (find-variables [{:open "a"} {:close "a"}]))))

  (testing "Variables from simple subsitutions"
    (is (= ["a"] (find-variables [{:cmd :cmd/echo :expression '[:plus a 1]}]))))

  (testing "Variables from if conditions"
    (is (= ["a"] (find-variables [{:cmd :cmd/if :condition '[:eq a 1]}]))))

  (testing "Variables from if branches"
    (is (= ["x"] (find-variables [{:cmd :cmd/if :condition '3
                                   :stencil.cleanup/blocks [[] [{:cmd :cmd/echo :expression 'x}]]}]))))

  (testing "Variables from loop expressions"
    (is (= ["xs" "xs[]"]
           (find-variables '[{:cmd :cmd/for, :variable y, :expression xs,
                              :stencil.cleanup/blocks [[{:cmd :cmd/echo, :expression [:plus y 1]}]]}])))
    (is (= ["xs" "xs[]" "xs[][]"]
           (find-variables '[{:cmd :cmd/for, :variable y, :expression xs
                              :stencil.cleanup/blocks [[{:cmd :cmd/for :variable w :expression y
                                         :stencil.cleanup/blocks [[{:cmd :cmd/echo :expression [:plus 1 w]}]]}]]}])))
    (is (= ["xs" "xs[].z.k"]
           (find-variables
            '[{:cmd :cmd/for :variable y :expression xs
               :stencil.cleanup/blocks [[{:cmd :cmd/echo :expression [:plus [:get y "z" "k"] 1]}]]}]))))

  (testing "Variables from loop bindings and bodies"
    ;; TODO: impls this test
)
  (testing "Variables from loop bodies (nesting)"
    ;; TODO: impls this test
)

  (testing "Nested loop bindings"
    (is (= ["xs" "xs[].t" "xs[].t[].n"]
           (find-variables
            '[{:cmd :cmd/for :variable a :expression xs
               :stencil.cleanup/blocks [[{:cmd :cmd/for :variable b :expression [:get a "t"]
                          :stencil.cleanup/blocks [[{:cmd :cmd/echo :expression [:plus [:get b "n"] 1]}]]}]]}])))
    ))

(deftest test-process-if-then-else
  (is (=
       '[{:open :body}
         {:open :a}
         {:cmd :cmd/if, :condition a,
          :branch/then [{:close :a}
                 {:open :a}
                 {:text "THEN"}
                 {:close :a}
                 {:open :a, :attrs {:a "three"}}],
          :branch/else ()}
         {:close :a}
         {:close :body}]

       (:executable (process '({:open :body}
                               {:open :a}
                               {:cmd :cmd/if, :condition a}
                               {:close :a}
                               {:open :a}
                               {:text "THEN"}
                               {:close :a}
                               {:open :a, :attrs {:a "three"}}
                               {:cmd :cmd/end}
                               {:close :a}
                               {:close :body}))))))

(deftest test-process-if-nested
  (is (=
       [<a>
        {:cmd :cmd/if, :condition '[:get x "a"],
         :branch/then [<／a>
                {:cmd :cmd/if, :condition '[:get x "b"],
                 :branch/then [<a> {:text "THEN"}]
                 :branch/else [<a>]}
                <／a>]
         :branch/else ()}]
       (:executable
        (process
         [<a>
          ,,{:cmd :cmd/if, :condition '[:get x "a"]}
          <／a>
          {:cmd :cmd/if, :condition '[:get x "b"]}
          <a>
          ,,{:text "THEN"}
          ,,{:cmd :cmd/end}
          <／a>
          {:cmd :cmd/end}])))))

:OK
