(ns segment-xref.core
  (:gen-class)
  (:require [cli-matic.core :refer [run-cmd]])
  (:require [clojure.data.csv :as csv])
  (:require [clojure.data.json :as json])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [engage.core :as engage])
  (:require [engage.consumers :as consumers])
  (:require [engage.search :as search]))

(defn segment-member-recorder
 "Function to consume the search status for while processing
 supporters for the provided segment.

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

(defn supporters
  "A supporter consumer accepts list of supporterIds.  For each supporter ID:
   1. looks up the associated segments,
   2. does some formatting, and
   3. writes a record containing the supporter info and the list of segments.
   Presumes that all segments for a supporter are in the same record.
   (Presumption borne out by documentation and observation.)
    @param slice  {list}  list of Engage supporter IDs
    @param spec   {hash}  specification provided by the configuration YAML file
    @param stash  {hash}  storage keyed by supporterId"
  [slice spec stash]
  (loop [a slice]
    (let [s (first a)
          token (:intToken spec)
          request-payload {:identifiers [(:supporterId s)]
                           :identifierType "SUPPORTER_ID"
                           :offset 0
                           :count 10}]

      (defn segments-results-proxy [slice]
        (loop [a slice]
          (let [result (first a)
                segments (:segments result)
                segment-text (segments-handler segments (:segmentId spec))
                record [[(:supporterId s) segment-text]]]
            (csv/write-csv (:writer stash) record))
          (when (not (empty? (rest a)))
            (recur (rest a)))))

      (search/supporter-segment-search
       token
       request-payload
       segments-results-proxy
       nil)
      (when (not (empty? (rest a)))
        (recur (rest a))))))

(defn run
  "Produce a CSV of supporters and the groups that they belong to.
    Supporters all belong to a single group thats specified in the
    YAML file as 'segmentId'."

  [{:keys [login segmentId csvFile]}]
  (let [spec (engage/use-yaml login)
        token (:intToken spec)
        segmentId segmentId
        request-payload {:segmentId segmentId
                         :offset 0
                         :count 10}
        writer (io/writer csvFile)
        stash {:writer writer}]

    (defn supporters-proxy [slice] (supporters slice spec stash))

    (csv/write-csv (:writer stash) [["supporterId" "segmentNames"]])
    (doall (search/segment-member-search
     token
     request-payload
     supporters-proxy
     segment-member-recorder))))

;; cli-matic configuration.  cli-matic made building this app a snap.
(def CONFIGURATION
  {:command     "segment_xref"
   :description "Create a CSV of supporters in a group and the other groups to which they belong."
   :version     "0.0.1"
   :opts        [{:as      "YAML configuration file"
                  :default nil
                  :option  "login"
                  :type    :string}
                 {:option   "segmentId"
                  :as      "SegmentId of interest."
                  :type    :string}
                 {:option   "csvFile"
                  :as       "CSV output of supporters and groups"
                  :default  "supporter_xref_results.csv"
                  :type     :string}]
   :runs run})

(defn -main
  [& args]
  (try
    (run-cmd args CONFIGURATION)
    (catch Exception e
      (printf "Error: %s\n" e)
      (flush))))
