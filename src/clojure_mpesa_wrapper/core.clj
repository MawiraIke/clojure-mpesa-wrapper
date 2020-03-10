(ns clojure-mpesa-wrapper.core
  (:require [clojure.data.json :refer [read-str]]
            [clj-http.client :as http]
            [clojure-mpesa-wrapper.Keys.keys :as ks])
  (:import (java.util Base64)
           (java.nio.charset StandardCharsets)
           (java.nio ByteBuffer)))


(defn auth [client-key client-secret]
  (let [url "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
        {:keys [body]} (http/get url {:basic-auth [client-key client-secret]})]
    (read-str body :key-fn keyword)))
;;(auth (:key ks/db) (:secret ks/db))

;; Return timestamp in the format "YYYYMMddHHmmss"
(defn format-to-timestamp [time]
  (.replaceAll (.format (java.text.SimpleDateFormat. "YYYYMMdd HHmmss") time) "\\s" ""))

;; Encode string, base64
(defn encode [to-encode]
  (let [message-bytes (.getBytes to-encode)
        encoder (Base64/getEncoder)]
    (.encodeToString encoder message-bytes)))

;; Lipa na mpesa,
;; Only works with Pay Bill. Buy Goods is not currently supported by the API
(defn lipa-na-mpesa [{:as   details-map
                      :keys [business-short-code transaction-type amount phone-number
                             callback-url account-reference transaction-description]
                      :or   {account-reference       "account"
                             transaction-type        "CustomerPayBillOnline"
                             transaction-description "Lipa na Mpesa Online"}}]
  (cond
    (not (.startsWith phone-number "254"))
    (throw (AssertionError. "Phone number is required to start with 254"))

    (not= (count phone-number) 12)
    (throw (AssertionError. "This phone number seems to be invalid"))

    (< amount 1)
    (throw (AssertionError. "Amount should be at least Ksh 1"))

    :default
    (let [url "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
          time-stamp (format-to-timestamp (java.util.Date.))
          raw-password (str business-short-code (:pass-key ks/db) time-stamp)
          encoding (encode raw-password)
          {:keys [body]}
          (clj-http.client/post
            url
            {:headers     {"Content-Type" "application/json"}
             :oauth-token "ACCESS_TOKEN"
             :body        (clojure.data.json/write-str
                            {:BusinessShortCode business-short-code
                             :Password          encoding
                             :Timestamp         time-stamp
                             :TransactionType   transaction-type
                             :Amount            amount
                             :PartyA            phone-number
                             :PartyB            business-short-code
                             :PhoneNumber       phone-number
                             :CallBackURL       callback-url
                             :AccountReference  account-reference
                             :TransactionDesc   transaction-description})})]
      (read-str body :key-fn keyword))))



















































(defn -main []
  (print "About to auth here"))
