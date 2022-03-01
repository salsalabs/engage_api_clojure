(ns engage.metrics
  (:gen-class)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as client]))

(defn get-metrics
  "Read Engage metrics. Return the metrics as a hash. Throws exceptions.

   @param   {string}  token            Engage Integration API token
   @returns {hash}    current Engage metrics object"
  [token]

  {:pre? (some token)}
  (let [url "https://api.salsalabs.org/api/integration/ext/v1/metrics"
        resp (client/get url {:headers {:content-type "application/json" :authToken token}})
        jbody (json/read-str (:body resp) :key-fn keyword)]
    (:payload jbody)))
