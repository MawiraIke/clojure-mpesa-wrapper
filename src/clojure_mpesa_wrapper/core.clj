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
;;   :short-code,                 Required, The organization shortcode used to receive the transaction
;;   :transaction-type,           Optional, Transaction type, default, "CustomerPayBillOnline"
;;                                The only supported type is "CustomerPayBillOnline"
;;   :amount,                     Required, int, The amount to be transacted
;;   :phone-number,               Required, The MSISDN sending the funds.
;;   :callback-url,               Required, The url to where responses from M-Pesa will be sent to.
;;   :account-reference,          Optional, Used with M-Pesa PayBills, default, "account"
;;   :transaction-description     Optional, A description of the transaction, default, "Lipa na Mpesa Online".
;;                                Must be less than 20 characters
;;   :passkey                     Optional, Lipa na mpesa pass key.
(defn lipa-na-mpesa [{:as   details-map
                      :keys [short-code transaction-type amount phone-number
                             callback-url account-reference transaction-description passkey]
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
          raw-password (str short-code passkey time-stamp)  ;; todo change the key
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
;;   :initiator -              Required, The credential/username used to authenticate the transaction request
;;   :short-code -             Required, The short code of the organization receiving the funds
;;   :remarks -                Optional, Comments that are sent along with the transaction
;;   :queue-url -              Required, The path that stores the information of a time out transaction
;;   :result-url -             Required, The path that receives a successful transaction
;;   :security-credential -    Required, Base64 encoded string of the M-Pesa short code and password, which is
;;                             encrypted using M-Pesa public key and validates the transaction on
;;                             M-Pesa Core system.
(defn balance [{:keys [initiator short-code remarks queue-url result-url security-credential] :or {remarks "Checking account balance"}}]
  (let [url "https://sandbox.safaricom.co.ke/mpesa/accountbalance/v1/query"
        {:keys [body]}
        (http/post url
                   {:headers     {"Content-Type" "application/json"}
                    :oauth-token "ACCESS_TOKEN"
                    :body        (write-str {:Initiator          initiator
                                             :SecurityCredential security-credential
                                             :CommandID          "AccountBalance"
                                             :PartyA             short-code
                                             :IdentifierType     "4"
                                             :Remarks            remarks
                                             :QueueTimeOutURL    queue-url
                                             :ResultURL          result-url})})]
    (read-str body :key-fn keyword)))

;; C2B API
;; The API enables PayBill and Buy Goods merchants to integrate M-Pesa and receive real time payments notifications


;; C2B Register
;; Registers 3rd party's confirmation and validation URLs to M-Pesa which then maps these URLs to the 3rd party
;; short-code. Whenever M-Pesa receives a transaction on the short-code, M-Pesa triggers a validation request
;; against the validation URL and the 3rd party system responds to M-Pesa with a validation response (either
;; a success or an error code). The response expected is the success code the 3rd party.
;; M-Pesa completes or cancels the transaction depending on the validation response it receives from the 3rd
;; party system. A confirmation request of the transaction is then sent by M-Pesa through the confirmation URL
;; back to the 3rd party which then should respond with a success acknowledging the confirmation.
;; Params:
;;   :short-code - The short code of the organization.
;;   :response-type - Default response type for timeout.
;;   :confirmation-url - Confirmation URL for the client.
;;   :validation-url - Validation URL for the client.
;;
;; The expected response contains the following parameters
;;   ConversationID - 	          A unique numeric code generated by the M-Pesa system of the response to a request.
;;   OriginatorConversationID -  	A unique numeric code generated by the M-Pesa system of the request.
;;   ResponseDescription -      	A response message from the M-Pesa system accompanying the response to a request.
(defn c2b [{:keys [short-code response-type confirmation-url validation-url]}]
  (let [{:keys [body]}
        (http/post
          "https://sandbox.safaricom.co.ke/mpesa/c2b/v1/registerurl"
          {:headers     {"Content-Type" "application/json"}
           :oauth-token "ACCESS_TOKEN"
           :body        (write-str {:ShortCode       short-code
                                    :ResponseType    response-type
                                    :ConfirmationURL confirmation-url
                                    :ValidationURL   validation-url})})]
    (read-str body :key-fn keyword)))


;; Params
;;   :short-code -      6 digit M-Pesa Till Number or PayBill Number.
;;   :command-id -      Unique command for each transaction type.
;;   :amount     -      The amount been transacted.
;;   :msisdn     - 	    MSISDN (phone number) sending the transaction, start with country code without the plus(+) sign.
;;   :bill-ref-number - Optional, Bill Reference Number
;;
;; The response contains the following parameters
;;   ConversationID -               A unique numeric code generated by the M-Pesa system of the response to a request.
;;   OriginatorConversationID -     A unique numeric code generated by the M-Pesa system of the request.
;;   ResponseDescription -          A response message from the M-Pesa system accompanying the response to a request.
(defn c2b-sim [{:keys [short-code command-id amount msisdn bill-ref-number]
                :or   {command-id "CustomerPayBillOnline"}}]
  (let [{:keys [body]}
        (http/post "https://sandbox.safaricom.co.ke/mpesa/c2b/v1/simulate"
                   {:headers     {"Content-Type" "application/json"}
                    :oauth-token "ACCESS_TOKEN"
                    :body        (write-str {:ShortCode     short-code
                                             :CommandID     command-id
                                             :Amount        amount
                                             :Msisdn        msisdn
                                             :BillRefNumber bill-ref-number})})]
    (read-str body :key-fn keyword)))
















































(defn -main []
  (print "About to auth here"))
