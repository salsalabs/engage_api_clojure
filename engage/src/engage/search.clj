(ns engage.search
  (:gen-class)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as client])
  (:require [engage.metrics :as metrics]))

(defn common-search
  "Engage uses POSTs to search for data. The generic search accepts
   a token, request payload and endpoint, then returns a hash containing
   the response returned from Engage.

   @param   {string}    token            Engage Integration API token
   @param   {string}    request-payload  Supporter search payload.
   @param   {string}    url              URL to use to search for records.
   @returns {hash}      contains the response body returned by Engage
   @throws  {Exception} Throws an exception containing JSON for any errors
                        returned by the endpoint

   @see https://api.salsalabs.org/help/integration#operation/supporterSearch
   @see https://api.salsalabs.org/api/integration/ext/v1/activities/search
   "
  [token request-payload url]
  {:pre? (and (some? token)
              (some? request-payload)
              (some? url))}

  (let [request-id nil
        request-header {:refId nil
                        :batchId nil
                        :userData nil}
        post-body {:id request-id
                   :header request-header
                   :payload request-payload}
        post-body-text (json/write-str post-body)
        resp (client/post url
                          {:headers {:content-type "application/json"
                                     :authToken token}
                           :body post-body-text})
        jbody (json/read-str (:body resp) :key-fn keyword)]
    (when (some? (:errors jbody))
      (let [errors (:errors jbody)
            text (json/pprint errors)]
        (throw (new Exception text))))
    jbody))

(defn generic-search
  "Uses {@link #common-search(token string, request-payload hash, url string)}
   to paginate through records.

   You provide a request payload that describes the records to return. Engage
   finds matching records and returns then in batches of (typically) 20 records.
   This function calls a function that you provide for each of the batches.
   This function also calls a function that lets you know the search progress.

   The search continues until all of the matching records are consumed.

   @param   {string}    token            Engage Integration API token
   @param   {string}    request-payload  Supporter search payload.
   @param   {string}    url              URL to use to search for records.
   @param   {function}  list-accessor    Function to return the list of things from the payload
   @param   {function}  consumer         Function that uses a list of records
   @param   {function}  recorder         Function to accept the current offset, the number
                                         of records and the total records.  Can be nil.
   @throws  {Exception} Throws an exception containing JSON for any errors
                        returned by the endpoint
   @see https://api.salsalabs.org/help/integration#operation/supporterSearch
   @see https://api.salsalabs.org/api/integration/ext/v1/activities/search
   @see https://api.salsalabs.org/api/integration/ext/v1/segments/members/search"
  [token request-payload url list-accessor consumer recorder]
  {:pre? (and (some? token)
              (some? request-payload)
              (some? url)
              (keyword? list-accessor)
              (some? consumer))}
  (let [metrics (metrics/get-metrics token)
        request-payload (assoc request-payload :count (:maxBatchSize metrics))]
    (loop [t token
           u url
           payload request-payload]
      ;; TODO: Do some error checking here.
      (let [response (common-search t payload u)
            response-payload (:payload response)
            ;; This is a bug.  THere should be offset, count and total.
            ;; There is only count.
            ;; That's why we use the offset from the request payload
            offset (:offset payload)
            current-count (:count response-payload)
            total (:total response-payload)
            records (list-accessor response-payload)]
        (when (not (nil? recorder))
          (recorder offset current-count total))
        (consumer records)
        (when (= current-count (:maxBatchSize metrics))
          (let [next-offset (+ offset current-count)
                next-payload (assoc payload :offset next-offset)]
            (recur t u next-payload)))))
    (flush)))

(defn activity-search
  "Uses {@link #generic-search(token payload url list-accessor consumer recorder)}
	to paginate through activity records.

   You provide a request payload that describes the activities to return.
   Engage finds matching records and returns them in batches of (typically)
   20 records. You provide a function that consumes batches. You may also
   provide a function that displays the current transfer status.

   The process continues until all of the matching records are consumed.

   @param   {string}    token            Engage Integration API token
   @param   {string}    request-payload  activity search payload.
   @param   {function}  consumer         Function that uses a list of activity records
   @param   {function}  recorder         Function to accept the current offset, the number
                                         of records and the total records. Can be nil
   @throws  {Exception} Throws an exception containing JSON for any errors
                        returned by the endpoint
   @see https://api.salsalabs.org/help/integration#operation/activitySearch"
  [token request-payload consumer recorder]

  (let [url "https://api.salsalabs.org/api/integration/ext/v1/activities/search"
        list-accessor :activities]
    (generic-search token request-payload url list-accessor consumer recorder)))

(defn email-search
  "Uses {@link #generic-search(token payload url list-accessor consumer recorder)}

   to paginate through email records.

   You provide a request payload that describes the activities to return.
   Engage finds matching records and returns them in batches of (typically)
   20 records. You provide a function that consumes batches of  emailActivity
   records. You may also provide a function that uses the current transfer
   status.

   The process continues until all of the matching records are consumed.

   @param   {string}    token            Engage Integration API token
   @param   {string}    request-payload  email search payload.
   @param   {function}  consumer         Function that uses a list of email records
   @param   {function}  recorder         Function to accept the current offset, the number
                                         of records and the total records. Can be nil
   @throws  {Exception} Throws an exception containing JSON for any errors
                        returned by the endpoint
   @see https://api.salsalabs.org/help/integration#operation/emailsSearch"
  [token request-payload consumer recorder]

  (let [url "https://api.salsalabs.org/api/integration/ext/v1/emails/search"
        list-accessor :emailActivities]
    (generic-search token request-payload url list-accessor consumer recorder)))

(defn segment-search
  "Uses {@link #generic-search(token payload url list-accessor consumer recorder)}

   to paginate through segment records.  A `segment` is a group of supporters.

   You provide a request payload that describes the segments to return.
   Engage finds matching records and returns them in batches of (typically)
   20 records. You provide a function that consumes lists of segment
   records. You may also provide a function that uses the current
   transfer status.

   The process continues until all of the matching records are consumed.

   @param   {string}    token            Engage Integration API token
   @param   {string}    request-payload  segment search payload.
   @param   {function}  consumer         Function that uses a list of segment records
   @param   {function}  recorder         Function to accept the current offset, the number
                                         of records and the total records. Can be nil
   @throws  {Exception} Throws an exception containing JSON for any errors
                        returned by the endpoint
  @see https://api.salsalabs.org/help/integration#operation/segmentSearch"
  [token request-payload consumer recorder]

  (let [url "https://api.salsalabs.org/api/integration/ext/v1/segments/search"
        list-accessor :segments]
    (generic-search token request-payload url list-accessor consumer recorder)))

(defn segment-member-search
  "Uses {@link #generic-search(token payload url list-accessor consumer recorder)}

   to accepts a segmentId and pages through the supporters that are in that segment.

   You provide a request payload that describes the segment to use for the
   search to return. Engage finds matching records and returns them in batches
   of (typically) 20 records. You provide a function that consumes lists of
   supporter records.You may also provide a function that uses the current
   transfer status.

   The process continues until all of the matching records are consumed.

   @param   {string}    token            Engage Integration API token
   @param   {string}    request-payload  Supporter segment search payload.
   @param   {function}  consumer         Function that uses a list of supporter records
   @param   {function}  recorder         Function to accept the current offset, the number
                                         of records and the total records. Can be nil
   @throws  {Exception} Throws an exception containing JSON for any errors
                        returned by the endpoint
   @see https://api.salsalabs.org/help/integration#operation/getSegmentMembers"
  [token request-payload consumer recorder]

  (let [url "https://api.salsalabs.org/api/integration/ext/v1/segments/members/search"
        list-accessor :supporters]
    (generic-search token request-payload url list-accessor consumer recorder)))

(defn supporter-search
  "Uses {@link #generic-search(token payload url list-accessor consumer recorder)}

   to paginate through supporter records.

   You provide a request payload that describes the supporters to return.
   Engage finds matching records and returns them in batches of (typically)
   20 records. You provide a function that consumes lists of supporter 
   records. You may also provide a function that uses the current transfer
   status.

   The process continues until all of the matching records are consumed.

   @param   {string}    token            Engage Integration API token
   @param   {string}    request-payload  Supporter search payload.
   @param   {function}  consumer         Function that uses a list of supporter records
   @param   {function}  recorder         Function to accept the current offset, the number
                                         of records and the total records. Can be nil
   @throws  {Exception} Throws an exception containing JSON for any errors
                        returned by the endpoint
   @see https://api.salsalabs.org/help/integration#operation/supporterSearch"
  [token request-payload consumer recorder]

  (let [url "https://api.salsalabs.org/api/integration/ext/v1/supporters/search"
        list-accessor :supporters]
    (generic-search token request-payload url list-accessor consumer recorder)))

(defn supporter-segment-search
  "Uses {@link #generic-search(token payload url list-accessor consumer recorder)}

  to accepts a single supporter ID and paginate through the list of segments
  that the supporter belongs 2.

  You provide a request payload that describes the supporter to use for the
  search to return. Engage finds matching records and returns them in batches
  of (typically) 20 records. You provide a function that consumes lists of 
  segment records.  You may also provide a function that uses the current
  transfer status.

  The process continues until all of the matching records are consumed.

  @param   {string}    token            Engage Integration API token
  @param   {string}    request-payload  Supporter segment search payload
  @param   {function}  consumer         Function that uses a list of supporter-segment records
  @param   {function}  recorder         Function to accept the current offset, the number
                                        of records and the total records. Can be nil
  @throws  {Exception} Throws an exception containing JSON for any errors
                        returned by the endpoint
  @see https://api.salsalabs.org/help/integration#operation/getGroupsForSupporters"
  [token request-payload consumer recorder]

  (let [url "https://api.salsalabs.org/api/integration/ext/v1/supporters/groups"
        list-accessor :results]
    (generic-search token request-payload url list-accessor consumer recorder)))
