(ns clojure-mpesa-wrapper.core
  (:require [clojure.data.json :refer [read-str]]
            [clj-http.client :as http]
            [clojure-mpesa-wrapper.Keys.keys :as ks])
  (:import (java.util Base64)))


;; Authenticate,
;; Expects a key and a secret, both should be strings.
(defn auth [client-key client-secret]
  (let [url "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
        {:keys [body]} (http/get url {:basic-auth [client-key client-secret]})]
    (read-str body :key-fn keyword)))

;; Return timestamp in the format "YYYYMMddHHmmss"
(defn format-to-timestamp [time]
  (.replaceAll (.format (java.text.SimpleDateFormat. "YYYYMMdd HHmmss") time) "\\s" ""))

;; Encode string, base64
(defn encode [to-encode]
  (let [message-bytes (.getBytes to-encode)
        encoder (Base64/getEncoder)]
    (.encodeToString encoder message-bytes)))

;; Lipa na mpesa,
;; Only works with Pay Bill. Buy Goods is not currently supported by the API.
;; Expected argument is a map containing key value pairs of:
;;   :business-short-code,        Required, The paybill number
;;   :transaction-type,           Optional, Transaction type, default, "CustomerPayBillOnline"
;;                                The only supported type is "CustomerPayBillOnline"
;;   :amount,                     Required, The amount to be transacted
;;   :phone-number,               Required, The MSISDN sending the funds.
;;   :callback-url,               Required, The url to where responses from M-Pesa will be sent to.
;;   :account-reference,          Optional, Used with M-Pesa PayBills, default, "account"
;;   :transaction-description     Optional, A description of the transaction, default, "Lipa na Mpesa Online"
(defn lipa-na-mpesa [{:as   details-map
                      :keys [business-short-code transaction-type amount phone-number
                             callback-url account-reference transaction-description]
                      :or   {account-reference       "account"
                             transaction-type        "CustomerPayBillOnline"
                             transaction-description "Lipa na Mpesa Online"}}]
  (cond
    (not (.startsWith phone-number "254")) (throw (AssertionError. "Phone number is required to start with 254"))
    (not= (count phone-number) 12) (throw (AssertionError. "This phone number seems to be invalid"))
    (< amount 1) (throw (AssertionError. "Amount should be at least Ksh 1"))
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
