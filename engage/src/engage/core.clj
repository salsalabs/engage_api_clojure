(ns engage.core
    (:require [java-time :as jt]
              [clj-yaml.core :as yaml]))

"We'll put constants and common utility things here..."

(defn java-date-to-engage
  "Accepts a java.util.Date (SDK 8) and returns an Engage-formatted
  date."
  [d]
  (let [instant (jt/instant d)
        zone-id (jt/zone-id "UTC")
        date (jt/local-date-time instant zone-id)
        formatted (jt/format "y-MM-dd'T'HH:MM:SS.s'0Z'" date)]
    formatted))

(defn missing-token-exception
    "Creates an exception for a missing missing token.
    @param {string} login       YAML filename
    @param {string} token-name  The missing token
    @returns {IllegalArgumentException} the exception"
    [login token-name]
      (let [message (format "File '%s' must contain token '%s'." login token-name)
            exception (Exception. message)]
        exception))

(defn use-yaml
  "Common function to use the contents of a YAML file. The Integration
  API and Web Developer API tokens are required.

  @param  login {string}            YAML file containing API tokens.
  @return                           Hash containing the API tokens.
  @throws Exception                 YAML parsing throws exceptions for malformed files
  @throws IllegalArgumentException  use-yaml throws exceptions for missing tokens"
  [login]
    (let [text (slurp login)
          options (yaml/parse-string text)]
     (cond
         (empty? (options :intToken)) (throw (missing-token-exception login "intToken"))
         (empty? (options :webToken)) (throw (missing-token-exception login "webToken"))
         :else options)))
