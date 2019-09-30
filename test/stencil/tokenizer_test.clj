(ns stencil.tokenizer-test
  (:require [stencil.merger :as m]
            [clojure.test :refer [deftest testing is]]))

(defn- run [s]
  (m/parse-to-tokens-seq (java.io.ByteArrayInputStream. (.getBytes (str s)))))

(deftest read-tokens-nested
  (testing "Read a list of nested tokens"
    (is (= (run "<a><b><c/></b><d></d></a>")
           [{:open :a}
            {:open :b}
            {:open+close :c}
            {:close :b}
            {:open+close :d}
            {:close :a}]))))

(deftest read-tokens-attributes-ns
  (testing "Namespace support in attributes"
    (is (= (run "<a one=\"1\" two=\"2\" xml:three=\"3\"></a>")
           [{:open+close :a
             :attrs {:one "1"
                     :two "2"
                     :xmlns.http%3A%2F%2Fwww.w3.org%2FXML%2F1998%2Fnamespace/three "3"}}]))))

(deftest read-tokens-echo
  (testing "Simple echo command"
    (is (= (run "<a>elotte {%=a%} utana</a>")
           [{:open :a}
            {:text "elotte "}
            {:cmd :echo :expression '(a)}
            {:text " utana"}
            {:close :a}]))))

(deftest read-tokens-if-then
  (testing "Simple conditional with THEN branch only"
    (is (= (run "<a>elotte {% if x%} akkor {% end %} utana</a>")
           [{:open :a}
            {:text "elotte "}
            {:cmd :if :condition '(x)}
            {:text " akkor "}
            {:cmd :end}
            {:text " utana"}
            {:close :a}]))))

(deftest read-tokens-if-then-else
  (testing "Simple conditional with THEN+ELSE branches"
    (is (= (run "<a>elotte {% if x%} akkor {% else %} egyebkent {% end %} utana</a>")
           [{:open :a}
            {:text "elotte "}
            {:cmd :if :condition '(x)}
            {:text " akkor "}
            {:cmd :else}
            {:text " egyebkent "}
            {:cmd :end}
            {:text " utana"}
            {:close :a}]))))

(deftest read-tokens-if-then-elif
  (testing "Simple conditional with THEN+ELIF branches"
    (is (= (run "<a>elotte{%if x%}akkor{%else if y%}de{%end%}utana</a>")
           '[{:open :a}
             {:text "elotte"}
             {:cmd :if :condition [x]}
             {:text "akkor"}
             {:cmd :else-if :condition [y]}
             {:text "de"}
             {:cmd :end}
             {:text "utana"}
             {:close :a}]))))

(deftest read-tokens-if-then-elif-else
  (testing "Simple conditional with THEN+ELIF+ELE branches"
    (is (= (run "<a>elotte{%if x%}akkor{%else if y%}de{%else%}egyebkent{%end%}utana</a>")
           '[{:open :a}
             {:text "elotte"}
             {:cmd :if :condition [x]}
             {:text "akkor"}
             {:cmd :else-if :condition [y]}
             {:text "de"}
             {:cmd :else}
             {:text "egyebkent"}
             {:cmd :end}
             {:text "utana"}
             {:close :a}]))))

(deftest read-tokens-unless-then
  (testing "Simple conditional with THEN branch only"
    (is (= (run "<a>{%unless x%} akkor {% end %}</a>")
           [{:open :a} {:cmd :if :condition '(x :not)} {:text " akkor "} {:cmd :end} {:close :a}]))))

(deftest read-tokens-unless-then-else
  (testing "Simple conditional with THEN branch only"
    (is (= (run "<a>{%unless x%} akkor {%else%} egyebkent {%end %}</a>")
           [{:open :a} {:cmd :if :condition '(x :not)} {:text " akkor "} {:cmd :else} {:text " egyebkent "} {:cmd :end} {:close :a}]))))

(deftest read-tokens-if-elif-then-else
  (testing "If-else if-then-else branching"
    (is (= '({:open :a} {:text "Hello "} {:cmd :if, :condition [x]} {:text "iksz"}
             {:cmd :else-if, :condition [y]} {:text "ipszilon"} {:cmd :else}
             {:text "egyebkent"} {:cmd :end} {:text  " Hola"} {:close :a})
           (run "<a>Hello {%if x%}iksz{%else if y%}ipszilon{%else%}egyebkent{%end%} Hola</a>")
           (run "<a>Hello {%if x%}iksz{%elseif y%}ipszilon{%else%}egyebkent{%end%} Hola</a>")
           (run "<a>Hello {%if x%}iksz{%elsif y%}ipszilon{%else%}egyebkent{%end%} Hola</a>")
           (run "<a>Hello {%if x%}iksz{%elif y%}ipszilon{%else%}egyebkent{%end%} Hola</a>")))))

(deftest read-tokens-for
  (testing "Simple loop"
    (is (= '[{:open :a} {:cmd :for, :expression [xs], :variable x}
             {:text "item"} {:cmd :end} {:close :a}]
           (run "<a>{%for x in xs%}item{% end %}</a>")))))

:OK
