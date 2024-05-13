(ns stencil.fs-test
  (:require [stencil.fs :as fs]
            [clojure.java.io :refer [file]]
            [clojure.test :refer [deftest testing is are]]))

(deftest test-directory?
  (is (thrown? NullPointerException (fs/directory? nil)))
  (is (true? (fs/directory? (file "/tmp"))))
  (testing "path does not exist"
    (is (false? (fs/directory? (file "/path/does/not/exist"))))
    (is (false? (fs/directory? (file "path/does/not/exist")))))
  (testing "relative path is resolved to directory"
    (is (true? (fs/directory? (file "src"))))))

(deftest test-unix-path
  (is (= nil (fs/unix-path nil)))
  (is (= "a" (fs/unix-path (file "a"))))
  (is (= "a/b/c" (fs/unix-path (file "a/b/c"))))
  (is (= "a/b" (fs/unix-path (file "a/b/"))))
  (testing "absolute path stays absolute"
    (is (= "/a/b" (fs/unix-path (file "/a/b"))))))

(deftest test-exists?
  (is (thrown? NullPointerException (fs/exists? nil)))
  (is (true? (fs/exists? (file "/tmp"))))
  (is (true? (fs/exists? (file "src"))))
  (is (false? (fs/exists? (file "/does/not-exist"))))
  (is (false? (fs/exists? (file "does/not-exist")))))