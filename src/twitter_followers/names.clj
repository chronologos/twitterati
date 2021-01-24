(ns twitter-followers.names
  (:require
   [environ.core :refer [env]]
   [clojure.data]
   [clojure.pprint]
   )
)

(def consumer-key
  (env :consumer-key))

(def consumer-secret
  (env :consumer-secret))

(def username
  (env :username))

(def state-filename
  "file for storing app state"
  "state.clj")

(def data-filename
  "file for storing edn data"
  "data.clj")

(def data-filename-name-only
  "file for storing edn data"
  "data-namesonly.clj")

(def data-filename-all-historical-names
  "file for storing edn data"
  "data-allnames.clj")

(def running-data-filename
  "file for storing aggregated statistical data over time"
  "running-data.txt")

(def running-diff-filename
  "file for storing diffs over time"
  "running-diff.txt")

(def follower-table-filename
  "file for storing a table of your followers"
  "followers-pretty.txt")

(def no-follow-back-filename
  "file for storing a table of your friends who don't follow you"
  "no-follow-back.clj")

(def followers-url
  ""
  "https://api.twitter.com/1.1/followers/list.json")

(def friends-url
  ""
  "https://api.twitter.com/1.1/friends/list.json")