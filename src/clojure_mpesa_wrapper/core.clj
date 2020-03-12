(ns clojure-mpesa-wrapper.core
  (:require [clojure.data.json :refer [read-str write-str]]
            [clj-http.client :as http]
            [clojure-mpesa-wrapper.Keys.keys :as ks])
  (:import (java.util Base64)))


;; Return timestamp in the format "YYYYMMddHHmmss"
(defn format-to-timestamp [time]
  (.replaceAll (.format (java.text.SimpleDateFormat. "YYYYMMdd HHmmss") time) "\\s" ""))

;; Encode string, base64
(defn encode [to-encode]
  (let [message-bytes (.getBytes to-encode)
        encoder (Base64/getEncoder)]
    (.encodeToString encoder message-bytes)))



;; ------------- API Methods


;; Authenticate,
;; Expects a key and a secret, both should be strings.
(defn auth [client-key client-secret]
  (let [url "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
        {:keys [body]} (http/get url {:basic-auth [client-key client-secret]})]
    (read-str body :key-fn keyword)))


;; Lipa na mpesa,
;; Only works with Pay Bill. Buy Goods is not currently supported by the API.
;; Expected argument is a map containing key value pairs of:
;;   :short-code,        Required, The paybill number
;;   :transaction-type,           Optional, Transaction type, default, "CustomerPayBillOnline"
;;                                The only supported type is "CustomerPayBillOnline"
;;   :amount,                     Required, int, The amount to be transacted
;;   :phone-number,               Required, The MSISDN sending the funds.
;;   :callback-url,               Required, The url to where responses from M-Pesa will be sent to.
;;   :account-reference,          Optional, Used with M-Pesa PayBills, default, "account"
;;   :transaction-description     Optional, A description of the transaction, default, "Lipa na Mpesa Online"
(defn lipa-na-mpesa [{:as   details-map
                      :keys [short-code transaction-type amount phone-number
                             callback-url account-reference transaction-description]
                      :or   {account-reference       "account"
                             transaction-type        "CustomerPayBillOnline"
                             transaction-description "Lipa na Mpesa Online"}}]
  (cond
    ;;(not (.startsWith phone-number "254")) (throw (IllegalArgumentException. "Phone number is required to start with 254"))
    ;;(not= (count phone-number) 12) (throw (IllegalArgumentException. "This phone number seems to be invalid"))
    ;;(not= (type amount) java.lang.Long) (throw (IllegalArgumentException. "Amount should be a number."))
    (< amount 1) (throw (AssertionError. "Amount should be at least Ksh 1"))
    :default
    (let [url "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
          time-stamp (format-to-timestamp (java.util.Date.))
          raw-password (str short-code (:pass-key ks/db) time-stamp) ;; todo change the key
          encoding (encode raw-password)
          {:keys [body]}
          (clj-http.client/post
            url
            {:headers     {"Content-Type" "application/json"}
             :oauth-token "ACCESS_TOKEN"
             :body        (clojure.data.json/write-str
                            {:BusinessShortCode short-code
                             :Password          encoding
                             :Timestamp         time-stamp
                             :TransactionType   transaction-type
                             :Amount            (str amount)
                             :PartyA            phone-number
                             :PartyB            short-code
                             :PhoneNumber       phone-number
                             :CallBackURL       callback-url
                             :AccountReference  account-reference
                             :TransactionDesc   transaction-description})})]
      (read-str body :key-fn keyword))))


;; Check Account balance
;; Use this API to enquire the balance on an M-Pesa BuyGoods (Till Number). Expects a
;; map with the following keys
;;   :initiator -              The credential/username used to authenticate the transaction request
;;   :short-code -             The short code of the organization receiving the funds
;;   :remarks -                Comments that are sent along with the transaction
;;   :queue-url -              The path that stores the information of a time out transaction
;;   :result-url -             The path that receives a successful transaction
(defn balance [{:keys [initiator short-code remarks queue-url result-url] :or {remarks "Checking account balance"}}]
  (let [security-credential (encode (str short-code (:pass-key ks/db))) ;; todo change the key to a more dynamic one
        url "https://sandbox.safaricom.co.ke/mpesa/accountbalance/v1/query"
        {:keys [body]}
        (http/post
          url
          {:headers     {"Content-Type" "application/json"}
           :oauth-token "ACCESS_TOKEN"
           :body        (write-str {
                                    :Initiator          initiator
                                    :SecurityCredential security-credential
                                    :CommandID          "AccountBalance"
                                    :PartyA             short-code
                                    :IdentifierType     "4"
                                    :Remarks            remarks
                                    :QueueTimeOutURL    queue-url
                                    :ResultURL          result-url
                                    })})]
    (read-str body :key-fn keyword)))




















































(defn -main []
  (print "About to auth here"))
