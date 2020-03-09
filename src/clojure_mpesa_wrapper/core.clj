(ns clojure-mpesa-wrapper.core
  (:require [clojure.data.json :as read-str]
            [clj-http.client :as http]
            [clojure-mpesa-wrapper.Keys.keys :as ks]))


(defn auth []
  (let [{:keys [body]} (http/get "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
                                 {:basic-auth [(:key ks/db) (:secret ks/db)]})]
    (read-str body :key-fn keyword)))




















































(defn -main []
  (print "About to auth here"))
