(ns engage.demo.core
  (:gen-class)
  (:require [cli-matic.core :refer [run-cmd]]
            [clojure.data.json :as json]
            [engage.consumers :as consumers]
            [engage.core :as engage]
            [engage.metrics :as metrics]
            [engage.search :as search]
            [java-time :as jt]))

(defn yaml-demo
  [{:keys [login]}]
  (println "yaml-demo: begin")
  (let [options (engage/use-yaml login)]
      (printf "yaml-demo: login filename is %s\n" login)
      (printf "yaml-demo: login filename contains these keys %s\n" (keys options))
      (printf "yaml-demo: token is '%s'\n", (:intToken options)))
    (println "yaml-demo: end"))

(defn activity-search-demo
  "Demonstrate activity search by retrieving activities and displaying
  a little bit of info for each activity.  Note that you can choose the
  activity type by using --activityType in the command line. Search is
  by last-modified date. Start and end last-modified are hard coded."
  [{:keys [login activityType modifiedFrom modifiedTo]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        typeText (clojure.string/upper-case (name activityType))
        modifiedFromText (engage/java-date-to-engage modifiedFrom)
        modifiedToText (engage/java-date-to-engage modifiedTo)
        request-payload {:modifiedFrom modifiedFromText
                         :modifiedTo modifiedToText
                         :type typeText
                         :offset 0
                         :count 10}]
    (search/activity-search
     token
     request-payload
     consumers/activity-consumer
     consumers/recorder)))

(defn email-search-demo
"Demostrates email activity by retrieving email blasts and interating
through the email activities for each blast. Start and end published
dates are hard coded."
[{:keys [login]}]

(let [spec (engage/use-yaml login)
      token (:intToken spec)
      request-payload {:publishedFrom "2020-04-01T00:00:00.000Z"
                       :publishedTo   "2021-04-01T00:00:00.000Z"
                       :type          "EMAIL"
                       :offset        0
                       :count         10}]
  (search/email-search
   token
   request-payload
   consumers/emailActivity-consumer
   consumers/recorder)))

(defn generic-search-demo
  "Demonstrate generic search by retreiving supporters and displaying
  a little bit of info for each supporter. Search is by last-modified date.
  Start and end last-modifieds are hard-coded."
  [{:keys [login]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        url "https://api.salsalabs.org/api/integration/ext/v1/supporters/search"
        request-payload {:modifiedFrom "2020-04-01T00:00:00.000Z"
                         :offset 0
                         :count 10
                         :includeNormal true}]
    (search/generic-search
     token
     request-payload url
     :supporters
     consumers/supporter-consumer
     consumers/recorder)))

(defn common-search-demo
  "Demonstrate the generic search by retrieving the first batch of supporters.
  Search is by the last-modified date.  Start and end last-modified dates are
  hard-coded. Returns the response as a hash (id, headers and payload...)"
  [{:keys [login]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        url "https://api.salsalabs.org/api/integration/ext/v1/supporters/search"
        request-payload {:modifiedFrom "2020-01-01T00:00:00.000Z"
                         :offset 0
                         :count 10
                         :includeNormal true}
        response (search/common-search
                  token
                  request-payload
                  url)]
    (json/pprint response)))

(defn metrics-demo
  "Reads metrics, displays the important ones, then displays them all."
  [{:keys [login]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        metrics (metrics/get-metrics token)]
    (println "Engage metrics demo")
    (printf "rateLimit:     %d\n" (:rateLimit metrics))
    (println "** `batchSize` is a key that doesn't exist.")
    (printf "batchSize:     %d\n" (:batchSize metrics))
    (printf "maxBatchSize:  %d\n" (:maxBatchSize metrics))
    (printf "totalAPICalls: %d\n" (:totalAPICalls metrics))
    (printf "\nAll metrics\n")
    (json/pprint metrics)))

(defn segment-search-demo
  "Demonstrate segment search by retreiving segments and displaying
  a little bit of info for each segment. Search is by last-modified date.
  Start and end last-modifieds are hard-coded."
  [{:keys [login]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        request-payload {:modifiedFrom "2000-04-01T00:00:00.000Z"
                         :offset 0
                         :count 10
                         :includeNormal true}]
    (search/segment-search
     token
     request-payload
     consumers/segment-consumer
     consumers/recorder)))

(defn segment-member-search-demo
  "Demonstrate segment-member search.  You provide a segmentId in the payload.
  Uses the supporter consumer to display members."
  [{:keys [login segmentId]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        request-payload {:segmentId segmentId
                         :offset 0
                         :count 10}]
    (search/segment-member-search
     token
     request-payload
     consumers/supporter-consumer
     consumers/recorder)))

(defn supporter-search-demo
  "Demonstrate supporter search by retrieving supporters and displaying
  a little bit of info for each supporter. Search is by last-modified date.
  Start and end last-modifieds are command-line options."
  [{:keys [login modifiedFrom modifiedTo]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        modifiedFromText (engage/java-date-to-engage modifiedFrom)
        modifiedToText (engage/java-date-to-engage modifiedTo)
        request-payload {:modifiedFrom modifiedFromText
                         :modifiedTo modifiedToText
                         :offset 0
                         :count 10
                         :includeNormal true}]
    (search/supporter-search
      token
      request-payload
      consumers/supporter-last-modified-consumer
      consumers/recorder)))

(defn supporter-segment-search-demo
  "Demonstrate paginating through segments for a supporter.  The output
  is ~10 lines of segment info for the provided supporterId. You
  provide an integration API token and a supporterId."
  [{:keys [login supporterId]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        supporter-ids (:supporterIds spec)
        request-payload {:identifiers supporterId
                         :identifierType "SUPPORTER_ID"
                         :offset 0
                         :count 10}]
    (search/supporter-segment-search
      token
      request-payload
      consumers/supporter-segment-consumer
      consumers/recorder)))

;; cli-matic configuration.  cli-matic made building this app a snap.
(def CONFIGURATION
  {:command     "clj-engage"
   :description "Demonstrates using the Engage API from Clojure"
   :version     "0.0.1"
   :opts        [{:option  "login"
                  :as      "YAML configuration file"
                  :default nil
                  :type    :string}]
   :subcommands [{:command     "metrics"
                  :description "Shows the API metrics for your account"
                  :runs        metrics-demo}

                 {:command     "common-search"
                  :description "Uses common-search to retrieve ~10 supporters"
                  :runs        common-search-demo}

                 {:command     "activity-search"
                  :description "Demonstrate search for activities"
                  :opts [{:option  "activityType"
                          :as      "Select activity type"
                          :default :fundraise
                          :type    #{ :subscription_management
                                      :subscribe
                                      :fundraise
                                      :petition
                                      :targeted_letter
                                      :regulation_comments
                                      :ticketed_event
                                      :p2p_event
                                      :facebook_ad}}
                        {:option    "modifiedFrom"
                         :as        "Modified from (YYYY-MM-DD)"
                         :default   (java.util.Date. (- 2021 1900) 1 1)
                         :type      :yyyy-mm-dd}
                        {:option    "modifiedTo"
                         :as        "Modified to (YYYY-MM-DD)"
                         :default   (java.util.Date. (- 2025 1900) 1 1)
                         :type      :yyyy-mm-dd}]
                  :runs        activity-search-demo}
                 {:command     "segment-search"
                  :description "Demonstrate search for segments (groups)"
                  :runs        segment-search-demo}

                 {:command     "segment-member-search"
                  :description "Demonstrate search for segment members"
                  :opts [{:option "segmentId"
                          :as     "The segmentId to search."
                          :required       true
                          :type           :string}]
                          :runs        segment-member-search-demo}

                 {:command     "supporter-search"
                  :description "Demonstrate search for supporters"
                  :opts [{:option    "modifiedFrom"
                          :as        "Modified from (YYYY-MM-DD)"
                          :default   (java.util.Date. (- 2021 1900) 1 1)
                          :type      :yyyy-mm-dd}
                         {:option    "modifiedTo"
                          :as        "Modified to (YYYY-MM-DD)"
                          :default   (java.util.Date. (- 2025 1900) 1 1)
                          :type      :yyyy-mm-dd}]
                  :runs        supporter-search-demo}

                 {:command     "supporter-segment-search"
                  :description "Demonstrate search for segments."
                  :opts [{:option   "supporterId"
                          :as      "SupporterId of interest. Can be used multiple times."
                          :multiple true
                          :type    :string}]
                  :runs        supporter-segment-search-demo}

                 {:command     "email-search"
                  :description "Demonstrate search for emails for email blasts"
                  :runs        email-search-demo}

                 {:command     "yaml-demo"
                  :description "Play with the engage/use-yaml function"
                  :runs        yaml-demo}]})

(defn -main
  [& args]
  (try
    (run-cmd args CONFIGURATION)
    (catch IllegalArgumentException e
      (printf "Error: %s\n" (:message e)))
    (catch Exception e
      (printf "Error: %s\n" (:message e)))
    (finally (flush))))
