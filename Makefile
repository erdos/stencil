.PHONY: clean compile lint test all
.DEFAULT_GOAL := all

clean:
	clojure -T:build clean

prepare:
	clojure -P

lint: clean
	clojure -M:lint/clj-kondo

pom: clean
	clojure -T:build pom

jar: clean
	clojure -T:build jar

uberjar: clean
	clojure -T:build uber

javadoc: clean
	clojure -T:build javadoc

compile: clean prepare
	clojure -T:build compile-java

clj-test: clean compile
    clojure -M:test

java-test: clean compile
    clojure -T:build java-test

coverage: clean prepare compile
	clojure -M:coverage

test: clean prepare compile clj-test java-test

all: clean compile lint test