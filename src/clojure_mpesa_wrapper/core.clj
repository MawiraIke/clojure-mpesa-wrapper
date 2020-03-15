(ns clojure-mpesa-wrapper.core
  (:require [clojure.data.json :refer [read-str write-str]]
            [clj-http.client :as http]))


;; Return timestamp in the format "YYYYMMddHHmmss"
(defn format-to-timestamp [time]
  (.replaceAll (.format (java.text.SimpleDateFormat. "YYYYMMdd HHmmss") time) "\\s" ""))

;; Encode string, base64
(defn encode [to-encode]
  (let [message-bytes (.getBytes to-encode)
        encoder (java.util.Base64/getEncoder)]
    (.encodeToString encoder message-bytes)))


;; ------------- API Methods


;; Authenticate,
;; Expects a key and a secret, both should be strings.
(defn auth [client-key client-secret]
  {:pre [(not= client-key nil) (not= client-secret nil) (string? client-key) (string? client-secret)]}
  (let [url "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
        {:keys [body]} (http/get url {:basic-auth [client-key client-secret]})]
    (read-str body :key-fn keyword)))



;; Check Account balance
;; Use this API to enquire the balance on an M-Pesa BuyGoods (Till Number). Expects a
;; map with the following keys
;;   :initiator -              Required, String, The credential/username used to authenticate the transaction request
;;   :short-code -             Required, Integer, The short code of the organization receiving the funds
;;   :remarks -                Optional, String, Comments that are sent along with the transaction
;;   :queue-url -              Required, String, The path that stores the information of a time out transaction
;;   :result-url -             Required, String, The path that receives a successful transaction
;;   :security-credential -    Required, String, Base64 encoded string of the M-Pesa short code and password, which is
;;                             encrypted using M-Pesa public key and validates the transaction on
;;                             M-Pesa Core system.
(defn balance [{:keys [initiator short-code remarks queue-url result-url security-credential]
                :or   {remarks "Checking account balance"}}]
  {:pre [(not= initiator nil) (not= security-credential nil) (not= short-code nil) (not= queue-url nil)
         (not= result-url nil) (string? initiator) (number? short-code) (when remarks (string? remarks))
         (string? queue-url) (string? result-url) (string? security-credential)]}
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



;; B2B request
;; This API enables Business to Business (B2B) transactions between a business and another business. Use of this API
;; requires a valid and verified B2B M-Pesa short code for the business initiating the transaction and the both
;; businesses involved in the transaction.
;; Expects a map with the following keys:
;;    :initiator - 	              Required, String, This is the credential/username used to authenticate the transaction request.
;;    :security-credential - 	    Required, String, Base64 encoded string of the B2B short code and password, which is encrypted using
;;                                M-Pesa public key and validates the transaction on M-Pesa Core system.
;;    :command-id -               Optional, String, Unique command for each transaction type, possible values are: BusinessPayBill,
;;                                MerchantToMerchantTransfer, MerchantTransferFromMerchantToWorking,
;;                                MerchantServicesMMFAccountTransfer, AgencyFloatAdvance
;;    :sender-identifier-type -   Optional, Number, Type of organization sending the transaction.
;;    :receiver-identifier-type - Optional, Number, Type of organization receiving the funds being transacted.
;;    :amount -                   Required, Number, The amount being transacted.
;;    :party-a -                  Required, Number, Organization’s short code initiating the transaction.
;;    :party-b -                  Required, Number, Organization’s short code receiving the funds being transacted.
;;    :account-reference -        Required, String, Account Reference mandatory for “BusinessPaybill” CommandID.
;;    :remarks -                  Optional, String, Comments that are sent along with the transaction.
;;    :queue-url -                Required, String, The path that stores information of time out transactions.it should be properly
;;                                validated to make sure that it contains the port, URI and domain name or publicly
;;                                available IP.
;;    :result-url -               Required, String, The path that receives results from M-Pesa it should be properly validated to make sure that it
;;                                contains the port, URI and domain name or publicly available IP.
(defn b2b [{:keys [initiator security-credential command-id sender-identifier-type receiver-identifier-type amount
                   party-a party-b account-reference remarks queue-url result-url]
            :or   {sender-identifier-type   4
                   receiver-identifier-type 4
                   command-id               "BusinessToBusinessTransfer"
                   remarks                  "B2B Request"}}]
  {:pre [(and (not= initiator nil) (string? initiator))
         (and (not= security-credential nil) (string? security-credential))
         (when command-id (string? command-id))
         (when sender-identifier-type (number? sender-identifier-type))
         (when receiver-identifier-type (number? receiver-identifier-type))
         (and (not= amount nil) (number? amount))
         (and (not= party-a nil) (number? party-a))
         (and (not= party-b nil) (number? party-b))
         (and (not= account-reference nil) (string? account-reference))
         (when remarks (string? remarks))
         (and (not= queue-url nil) (string? queue-url))
         (and (not= result-url nil) (string? result-url))]}
  (let [{:keys [body]}
        (http/post
          "https://sandbox.safaricom.co.ke/mpesa/b2b/v1/paymentrequest"
          {:headers     {"Content-Type" "application/json"}
           :oauth-token "ACCESS_TOKEN"
           :body        (write-str {:Initiator              initiator
                                    :SecurityCredential     security-credential
                                    :CommandID              command-id
                                    :SenderIdentifierType   sender-identifier-type
                                    :RecieverIdentifierType receiver-identifier-type
                                    :Amount                 amount
                                    :PartyA                 party-a
                                    :PartyB                 party-b
                                    :AccountReference       account-reference
                                    :Remarks                remarks
                                    :QueueTimeOutURL        queue-url
                                    :ResultURL              result-url})})]
    (read-str body :key-fn keyword)))



;; B2C Payment Request
;; This API enables Business to Customer (B2C) transactions between a company and customers who are the end-users of its
;; products or services. Use of this API requires a valid and verified B2C M-Pesa Short code.
;; Expects a map with the following keys:
;;   initiator-name - Required, String, This is the credential/username used to authenticate the transaction request.
;;   command-id -     Optional, String, Unique command for each transaction type e.g. SalaryPayment, BusinessPayment,
;;                    PromotionPayment
;;   amount -         Required, Number, The amount being transacted
;;   sender-party -   Required, Number, Organization’s shortcode initiating the transaction.
;;   receiver-party - Required, Number, Phone number receiving the transaction
;;   remarks -        Optional, String, Comments that are sent along with the transaction.
;;   queue-url -      Required, String, The timeout end-point that receives a timeout response.
;;   result-url -     Required, String, The end-point that receives the response of the transaction
;;   occasion -       Optional
(defn b2c [{:keys [initiator-name security-credential command-id amount sender-party receiver-party
                   remarks queue-url result-url occasion]
            :or   {command-id "BusinessPayment"
                   remarks    "B2C Payment"}}]
  {:pre [(not= initiator-name nil) (not= security-credential nil) (not= command-id nil)
         (not= amount nil) (not= sender-party nil) (not= receiver-party nil) (not= remarks nil)
         (not= queue-url nil) (not= result-url nil) (string? initiator-name) (when command-id (string? command-id))
         (number? amount) (number? sender-party) (number? receiver-party) (when remarks (string? remarks))
         (string? queue-url) (string? result-url)]}
  (let [{:keys [body]}
        (http/post "https://sandbox.safaricom.co.ke/mpesa/b2c/v1/paymentrequest"
                   {:headers     {"Content-Type" "application/json"}
                    :oauth-token "ACCESS_TOKEN"
                    :body        (write-str
                                   {:InitiatorName      initiator-name
                                    :SecurityCredential security-credential
                                    :CommandID          command-id
                                    :Amount             amount
                                    :PartyA             sender-party
                                    :PartyB             receiver-party
                                    :Remarks            remarks
                                    :QueueTimeOutURL    queue-url
                                    :ResultURL          result-url
                                    :Occasion           occasion})})]
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
;; Expects a map with the following keys:
;;   :short-code -       Required, Number, The short code of the organization.
;;   :response-type -    Required, String, Default response type for timeout.
;;   :confirmation-url - Required, String, Confirmation URL for the client.
;;   :validation-url -   Required, String, Validation URL for the client.
;;
;; The expected response contains the following parameters
;;   ConversationID - 	          A unique numeric code generated by the M-Pesa system of the response to a request.
;;   OriginatorConversationID -  	A unique numeric code generated by the M-Pesa system of the request.
;;   ResponseDescription -      	A response message from the M-Pesa system accompanying the response to a request.
(defn c2b-reg [{:keys [short-code response-type confirmation-url validation-url]}]
  {:pre [(and (not= short-code nil) (number? short-code))
         (and (not= response-type nil) (string? response-type))
         (and (not= confirmation-url nil) (string? validation-url))
         (and (not= validation-url nil) (string? validation-url))]}
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
;;   :short-code -      Required, Number, 6 digit M-Pesa Till Number or PayBill Number.
;;   :command-id -      Optional, String, Unique command for each transaction type.
;;   :amount     -      Required, Number, The amount been transacted.
;;   :msisdn     - 	    Required, Number, MSISDN (phone number) sending the transaction, start with country code without the plus(+) sign.
;;   :bill-ref-number - Required, String, Optional, Bill Reference Number
;;
;; The response contains the following parameters
;;   ConversationID -               A unique numeric code generated by the M-Pesa system of the response to a request.
;;   OriginatorConversationID -     A unique numeric code generated by the M-Pesa system of the request.
;;   ResponseDescription -          A response message from the M-Pesa system accompanying the response to a request.
(defn c2b-sim [{:keys [short-code command-id amount msisdn bill-ref-number]
                :or   {command-id "CustomerPayBillOnline"}}]
  {:pre [(and (not= short-code nil) (number? short-code))
         (when command-id (string? command-id))
         (and (not= amount nil) (number? amount))
         (and (not= msisdn nil) (number? msisdn))
         (and (not= bill-ref-number nil) (string? bill-ref-number))]}
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



;; Lipa na mpesa,
;; Only works with Pay Bill. Buy Goods is not currently supported by the API.
;; Expected argument is a map containing key value pairs of:
;;   :short-code,                 Required, Number, The organization short-code used to receive the transaction
;;   :transaction-type,           Optional, String, Transaction type, default, "CustomerPayBillOnline"
;;                                The only supported type is "CustomerPayBillOnline"
;;   :amount,                     Required, Number, The amount to be transacted
;;   :phone-number,               Required, Number, The MSISDN sending the funds.
;;   :callback-url,               Required, String, The url to where responses from M-Pesa will be sent to.
;;   :account-reference,          Optional, String, Used with M-Pesa PayBills, default, "account"
;;   :transaction-description     Optional, String, A description of the transaction, default, "Lipa na Mpesa Online".
;;                                Must be less than 20 characters
;;   :passkey                     Optional, String, Lipa na mpesa pass key.
(defn lipa-na-mpesa-online [{:keys [short-code transaction-type amount phone-number
                                    callback-url account-reference transaction-description passkey]
                             :or   {account-reference       "account"
                                    transaction-type        "CustomerPayBillOnline"
                                    transaction-description "Lipa na Mpesa Online"}}]
  {:pre [(and (not= short-code nil) (number? short-code))
         (when transaction-type (string? transaction-type))
         (and (not= amount nil) (number? amount) (>= amount 1))
         (and (not= phone-number nil) (number? phone-number) (.startsWith (str phone-number) "254")
              (= (count (str phone-number)) 12))
         (and (not= callback-url nil) (string? callback-url))
         (when account-reference (string? account-reference))
         (when transaction-description (string? transaction-description))
         (when passkey (string? passkey))]}
  (let [url "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
        time-stamp (format-to-timestamp (java.util.Date.))
        raw-password (str short-code passkey time-stamp)
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
                           :Amount            amount
                           :PartyA            phone-number
                           :PartyB            short-code
                           :PhoneNumber       phone-number
                           :CallBackURL       callback-url
                           :AccountReference  account-reference
                           :TransactionDesc   transaction-description})})]
    (read-str body :key-fn keyword)))



;; Reversal
;; Reverses a B2B, B2C or C2B M-Pesa transaction.
;; Expects a map with the following keys:
;;   :transaction-id -      Required, String, The transaction id for reversal eg LKXXXX1234
;;   :amount -              Required, Number, Amount being transacted
;;   :queue-url -           Required, String, The path that stores information of time out transaction.
;;   :result-url -          Required, String, The path that stores information of transaction.
;;   :short-code -          Required, String, Organization Receiving the funds.
;;   :remarks -             Optional, String, Comments that are sent along with the transaction.
;;   :occasion -            Optional, String, Optional Parameter
;;   :initiator -           Required, This is the credential/username used to authenticate the transaction request.
;;   :receiver-id-type -    Optional, String, Type of organization receiving the transaction
;;   :command-id -          Optional, String, Takes only 'TransactionReversal' Command id
;;   :security-credential - Required, String, Base64 encoded string of the M-Pesa short code and password, which is
;;                          encrypted using M-Pesa public key and validates the transaction on M-Pesa Core system.
(defn reversal [{:keys [transaction-id amount queue-url result-url short-code remarks occasion initiator
                        receiver-id-type command-id security-credential]
                 :or   {remarks          "Reversal"
                        occasion         "Reversal"
                        receiver-id-type "4"
                        command-id       "TransactionReversal"}}]
  (let [{:keys [body]}
        (http/post "https://sandbox.safaricom.co.ke/mpesa/reversal/v1/request"
                   {:headers     {"Content-Type" "application/json"}
                    :oauth-token "ACCESS_TOKEN"
                    :body        (write-str
                                   {:Initiator              initiator
                                    :SecurityCredential     security-credential
                                    :CommandID              command-id
                                    :TransactionID          transaction-id
                                    :Amount                 amount
                                    :ReceiverParty          short-code
                                    :RecieverIdentifierType receiver-id-type
                                    :ResultURL              result-url
                                    :QueueTimeOutURL        queue-url
                                    :Remarks                remarks
                                    :Occasion               occasion})})]
    (read-str body :key-fn keyword)))






