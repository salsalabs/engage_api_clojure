(ns engage.consumers
  (:gen-class)
  (:require [clojure.data.json :as json]))

(defn activity-consumer
  "An example of a function to consume a list of activity records.

  @param  slice {list} list of Activity records
  @see https://api.salsalabs.org/api/integration/ext/v1/activities/search"
  [slice]
  (loop  [a slice]
    (let [s (first a)
          d (get (.split (:activityDate s) "T") 0)]
      (printf "%-36s %-23s %-20s %-30s %-30s %s\n"
              (:activityId s)
              (:activityType s)
              (:activityFormName s)
              (:personName s)
              (:personEmail s)
              d)
      (when (not-empty (rest a))
        (recur (rest a))))))

(defn emailActivity-consumer
  "An example of a function to consume a list of emailActivity records.
  For the record, an 'email activity' is what's commonly known as an
  'email blast'.

  @param  slice {list} list of Engage emailActivity records
  @see https://api.salsalabs.org/help/integration#operation/emailsSearch"
  [slice]
  (loop [a slice]
    (let [s (first a)
          d (get (.split (:publishDate s) "T") 0)]
      (printf "%-36s %-23s %-25s %-25s %-50s %s\n"
              (:id s)
              (:topic s)
              (:publishDate s)
              (:scheduleTime s)
              (:name s)
              (:description s)
              d)
      (when (not-empty (rest a))
        (recur (rest a))))))

(defn segment-consumer
  "An example of a function to consume a list of segment records.

  @param  slice {list} list of Engage segments
  @see https://api.salsalabs.org/api/integration/ext/v1/segment/search"
  [slice]
  (let [sorted-slice (sort-by :name slice)]
    (loop [a sorted-slice]
      (let [s (first a)
            segmentId (:segmentId s "")
            name (:name s "")
            type (:type s "")
            mailingList (:mailingList s "")
            publicName (:publicName s "")
            totalMembers (:totalMembers s -1)]
        (printf "%-36s %-60s %-8s %10s %-20s %5d\n"
                segmentId
                name
                type
                mailingList
                publicName
                totalMembers)
        (when (not-empty (rest a))
          (recur (rest a)))))))

(defn supporter-consumer
  "An example of a function to consume a list of supporter records.

  @param  slice {list} a list of Engage supporter records
  @see https://api.salsalabs.org/api/integration/ext/v1/supporter/search"
  [slice]
  (loop [a slice]
    (let [s (first a)
          full-name (str (:firstName s) " " (:lastName s))
          contacts (:contacts s)
          contact (get contacts 0)
          method (str (:type contact) ": " (:value contact))]
      (printf "%-36s %-16s %-32s\n" (:supporterId s) full-name method))
    (when (not-empty (rest a))
      (recur (rest a)))))

(defn supporter-segment-consumer
  "An example of a function to consume a list of SupporterSegment records.
   The record is composed of supporterId and a list of segments.
   This consumer shows the supporterId and some basic segment info.

  @param  slice {list} list of Engage supporter-segments records
  @see https://api.salsalabs.org/help/integration#operation/getGroupsForSupporters"
  [slice]
  (loop [a slice]
    (let [s (first a)
          supporterId (:supporterId s)
          segments (:segments s)]
      (printf "supporterId: %s is in these (%d) segments\n", supporterId (count segments))
      (segment-consumer segments))
    (when (not-empty (rest a))
      (recur (rest a)))))

(defn recorder
  "Function to consume the current search status.

   @param  {number}  search offset
   @param  {number}  number of records returned
   @param  {number}  total records to return"
  [offset count total]
  (printf "recorder: offset: %6d, count: %6d, total:  %6d\n" offset count total))
