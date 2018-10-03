(ns stencil.cleanup-test
  (:require [clojure.test :refer [deftest testing is are]]
            [stencil
             [cleanup :refer :all]
             [types :refer :all]]))

(deftest stack-revert-close-test
  (testing "Egyszeru es ures esetek"
    (is (= [] (stack-revert-close [])))
    (is (= [] (stack-revert-close nil)))
    (is (= [] (stack-revert-close [{:whatever 1} {:else 2}]))))
  (testing "A kinyito elemeket be kell csukni"
    (is (= [(->CloseTag "a") (->CloseTag "b")]
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
      [{:cmd       :if :condition 1}
       {:text      "Then"}
       {:cmd       :end}]
      [{:cmd :if :condition 1
        :blocks [{:children [{:text "Then"}]}]}]

      ;; if-then-else-fi
      [{:cmd :if :condition 1}
       {:text "Then"}
       {:cmd :else}
       {:text "Else"}
       {:cmd :end}]
      [{:cmd :if, :condition 1
        :blocks [{:children [{:text "Then"}]} {:children [{:text "Else"}]}]}])))

(deftest normal-ast-test-1
  (is (= (control-ast-normalize
          (annotate-environments
           [(->OpenTag "html")
            (->OpenTag "a")
            (->TextTag "Inka")
            {:cmd :if
             :blocks [{:children [(->TextTag "ikarusz")
                                  (->CloseTag "a")
                                  (->TextTag "bela")
                                  (->OpenTag "b")
                                  (->TextTag "Hello")]}
                      {:children [(->TextTag "Virag")
                                  (->CloseTag "b")
                                  (->TextTag "Hajdiho!")
                                  (->OpenTag "c")
                                  (->TextTag "Bogar")]}]}
            (->TextTag "Kaktusz")
            (->CloseTag "c")
            (->CloseTag "html")]))

         [(->OpenTag "html")
          (->OpenTag "a")
          (->TextTag "Inka")
          {:cmd :if
           :then [(->TextTag "ikarusz")
                  (->CloseTag "a")
                  (->TextTag "bela")
                  (->OpenTag "b")
                  (->TextTag "Hello")
                  (->CloseTag "b")
                  (->OpenTag "c")]
           :else [(->CloseTag "a")
                  (->OpenTag "b")
                  (->TextTag "Virag")
                  (->CloseTag "b")
                  (->TextTag "Hajdiho!")
                  (->OpenTag "c")
                  (->TextTag "Bogar")]}
          (->TextTag "Kaktusz")
          (->CloseTag "c")
          (->CloseTag "html")])))

(def <i> (->open "i")) (def <／i> (->close "i"))
(def <b> (->open "b")) (def <／b> (->close "b"))
(def <j> (->open "j")) (def <／j> (->close "j"))
(def <a> (->open "a")) (def <／a> (->close "a"))

(deftest normal-ast-test-0
  (testing "Amikor a formazas a THEN blokk kozepeig tart, akkor az ELSE blokk-ba is be kell tenni a lezaro taget."
    (is (= (control-ast-normalize
            (annotate-environments
             [{:cmd :if
               :blocks [{:children [(->text "bela") <b> (->text "Hello")]}
                        {:children [(->text "Virag")]}]}
              (->close "b")]))

           [{:cmd :if
             :then [(->text "bela") <b> (->text "Hello")]
             :else [<b> (->text "Virag")]}
            (->close "b")]))))

(deftest normal-ast-test-0-deep
  (testing "Amikor a formazas a THEN blokk kozepeig tart, akkor az ELSE blokk-ba is be kell tenni a lezaro taget."
    (is (= (control-ast-normalize
            (annotate-environments
             [{:cmd :if
               :blocks [{:children [(->text "bela") <b> <i> (->text "Hello") <／i>]}
                        {:children [<j> (->text "Virag") <／j>]}]}
              <／b>]))

           [{:cmd :if
             :then [(->text "bela") <b> <i> (->text "Hello") <／i>]
             :else [<b> <j> (->text "Virag") <／j>]}
            <／b>]))))

(deftest normal-ast-test-condition-only-then
  (testing "Az elagazasban eredeetileg csak THEN ag volt de beszurjuk az else agat is."
    (is (= (control-ast-normalize
            (annotate-environments
             [<a>
              {:cmd :if
               :blocks [{:children [(->text "bela") <b> <i> (->text "Hello") <／i>]}]}
              <／b>
              <／a>]))

           [<a> {:cmd :if
                 :then [(->text "bela") <b> <i> (->text "Hello") <／i>]
                 :else [<b>]}
            <／b>
            <／a>]))))

(defn >>for-loop [& children] {:cmd :for :blocks [{:children (vec children)}]})

(deftest test-normal-ast-for-loop-1
  (testing "ismetleses ciklusban"
    (is (= (control-ast-normalize
            (annotate-environments
             [<a>
              (->text "before")
              (>>for-loop (->text "inside1") <／a> <b> (->text "inside2"))
              (->text "after")
              <／b>]))
           [<a>
            (->text "before")
            {:cmd :for
             :body-run-none [<／a> <b>]
             :body-run-once [(->text "inside1") <／a> <b> (->text "inside2")]
             :body-run-next [<／b> <a> (->text "inside1") <／a> <b> (->text "inside2")]}
            (->text "after")
            <／b>]))))

(deftest test-find-variables
  (testing "Empty test cases"
    (is (= () (find-variables [])))
    (is (= () (find-variables nil)))
    (is (= () (find-variables [{:open "a"} {:close "a"}]))))

  (testing "Variables from simple subsitutions"
    (is (= ["a"] (find-variables [{:cmd :echo :expression '[a 1 :plus]}]))))

  (testing "Variables from if conditions"
    (is (= ["a"] (find-variables [{:cmd :if :condition '[a 1 :eq]}]))))

  (testing "Variables from if branches"
    (is (= ["x"] (find-variables [{:cmd :if :condition []
                                   :blocks [[] [{:cmd :echo :expression '[x]}]]}]))))

  (testing "Variables from loop expressions"
    (is (= ["xs" "xs[]"]
           (find-variables '[{:cmd :for, :variable y, :expression [xs],
                              :blocks [[{:cmd :echo, :expression [y 1 :plus]}]]}])))
    (is (= ["xs" "xs[]" "xs[][]"]
           (find-variables '[{:cmd :for, :variable y, :expression [xs]
                              :blocks [[{:cmd :for :variable w :expression [y]
                                         :blocks [[{:cmd :echo :expression [1 w :plus]}]]}]]}])))
    (is (= ["xs" "xs[].z.k"]
           (find-variables
            '[{:cmd :for :variable y :expression [xs]
               :blocks [[{:cmd :echo :expression [y.z.k 1 :plus]}]]}]))))

  (testing "Variables from loop bindings and bodies"
    ;; TODO: impls this test
)
  (testing "Variables from loop bodies (nesting)"
    ;; TODO: impls this test
)

  (testing "Nested loop bindings"
    ;; TODO: legyen e gymasban ket for ciklus meg egy echo?
))

(deftest test-process-if-then-else
  (is (=
       '[{:open :body}
         {:open :a}
         {:cmd :if, :condition [a],
          :then [{:close :a}
                 {:open :a}
                 {:text "THEN"}
                 {:close :a}
                 {:open :a, :attrs {:a "three"}}],
          :else ()}
         {:close :a}
         {:close :body}]

       (:executable (process '({:open :body}
                               {:open :a}
                               {:cmd :if, :condition [a]}
                               {:close :a}
                               {:open :a}
                               {:text "THEN"}
                               {:close :a}
                               {:open :a, :attrs {:a "three"}}
                               {:cmd :end}
                               {:close :a}
                               {:close :body}))))))

(deftest test-process-if-nested
  (is (=
       [<a>
        {:cmd :if, :condition '[x.a],
         :then [<／a>
                {:cmd :if, :condition '[x.b],
                 :then [<a> {:text "THEN"}]
                 :else [<a>]}
                <／a>]
         :else ()}]
       (:executable
        (process
         [<a>
          ,,{:cmd :if, :condition '[x.a]}
          <／a>
          {:cmd :if, :condition '[x.b]}
          <a>
          ,,{:text "THEN"}
          ,,{:cmd :end}
          <／a>
          {:cmd :end}])))))

:OK
