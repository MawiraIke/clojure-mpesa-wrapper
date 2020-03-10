(ns clojure-mpesa-wrapper.core-test
  (:require [clojure.test :refer :all]
            [clojure-mpesa-wrapper.core :refer :all]))

;; Requires a key and a secret to pass
(deftest authentication-test
  (testing "Authentication"
    (is (= (type (auth "<Enter key as a String" "<Enter secret as a String>"))
           clojure.lang.PersistentArrayMap))))
