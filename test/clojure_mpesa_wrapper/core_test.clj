(ns clojure-mpesa-wrapper.core-test
  (:require [clojure.test :refer :all]
            [clojure-mpesa-wrapper.core :refer :all]))

(deftest encode-test
  (testing "Base64 encoding"
    (is (= (encode (str "174379" "MTc0Mzc5YmZiMjc5ZjlhYTliZGJjZjE1OGU5N2RkNzFhNDY3Y2QyZTBjODkzMDU5YjEwZjc4ZTZiNzJhZGExZWQyYzkxOTIwMTgwNDA5MDkzMDAy" "20180409093002"))
           "MTc0Mzc5TVRjME16YzVZbVppTWpjNVpqbGhZVGxpWkdKalpqRTFPR1U1TjJSa056RmhORFkzWTJReVpUQmpPRGt6TURVNVlqRXdaamM0WlRaaU56SmhaR0V4WldReVl6a3hPVEl3TVRnd05EQTVNRGt6TURBeTIwMTgwNDA5MDkzMDAy"))))

;; Requires a key and a secret to pass
(deftest authentication-test
  (testing "Authentication"
    (is (= (type (auth "<Enter key as a String" "<Enter secret as a String>"))
           clojure.lang.PersistentArrayMap))))
