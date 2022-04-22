(ns supporter_segment.core
  (:gen-class)
  (:require [cli-matic.core :refer [run-cmd]])
  (:require [clojure.data.csv :as csv])
  (:require [clojure.data.json :as json])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [engage.core :as engage])
  (:require [engage.consumers :as consumers])
  (:require [engage.search :as search]))

(defn local-recorder
 "Function to consume the search status for while processing
 segments for the provided supporter.

  @param  {number}  search offset
  @param  {number}  number of records returned
  @param  {number}  total records to return"
 [offset count total]
 (printf "segment-members: offset: %6d, count: %6d, total:  %6d\n" offset count total))

(defn segments-handler
  "A segment consumer accepts a list of segments and appends them to
  to create a comma-separated and quote-delimited string of segments.
    @param slice    {list}    list of Engage segment records
    @param exclude  {string}  exclude this segmentId
    @returns        {string}  comma-separated segment names"
  [slice exclude]
  (let [names (doall (->> slice
                          (filter (fn [x] (not= exclude (:segmentId x))))
                          (map (fn [x] (:name x)))
                          (sort)))]
    (str/join ","  names)))

(defn run
  "Produce a CSV of groups to which a supporter belongs."

  [{:keys [login email csvFile]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        emails (list email)
        request-payload {:identifiers     emails
                         :identifierType  "EMAIL_ADDRESS"
                         :offset          0
                         :count           20}
        writer (io/writer csvFile)
        stash {:writer writer}]
    (csv/write-csv (:writer stash) [["segmentId"
                                     "name"
                                     "type"
                                     "mailingList"
                                     "publicName"
                                     "totalMembers"]])

    (doall (search/supporter-segment-search token
                                            request-payload
                                            consumers/supporter-segment-consumer
                                            local-recorder))))

;; cli-matic configuration.  cli-matic made building this app a snap.
(def CONFIGURATION
  {:command     "supporter_segment"
   :description "Create a CSV of segments to which the specified supporter belongs."
   :version     "0.0.1"
   :opts        [{:as        "YAML configuration file"
                    :default nil
                    :option  "login"
                    :type    :string}
                  {:option   "email"
                    :as      "Email address of the supporter of interest."
                    :type    :string}
                  {:option   "csvFile"
                    :as      "CSV output of groups that where the supporter is a member"
                    :default "supporter_segment_results.csv"
                    :type    :string}]
   :runs run})

(defn -main
  [& args]
  (try
    (run-cmd args CONFIGURATION)
    (catch Exception e
      (printf "Error: %s\n" e)
      (flush))))
