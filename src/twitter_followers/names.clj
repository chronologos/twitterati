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

; for unix filesystems
(def data-dir "../data/")

(def state-fpath
  "file for storing app state"
  (str data-dir "state.clj"))

(def data-fpath
  "file for storing edn data"
  (str data-dir "data.clj"))

(def follower-names-fpath
  "file for storing follower names"
  (str data-dir  "data-namesonly.clj"))

(def historical-followers-fpath
  "file for storing all historical follower names (even those who have unfollowed)"
  (str data-dir "data-allnames.clj"))

(def running-data-fpath
  "file for storing aggregated statistical data over time"
  (str data-dir "running-data.txt"))

(def running-diff-fpath
  "file for storing diffs over time"
  (str data-dir "running-diff.txt"))

(def follower-table-fpath
  "file for storing a table of your followers"
  (str data-dir "followers-pretty.txt"))

(def no-follow-back-fpath
  "file for storing a table of your follows who don't follow you"
 (str data-dir "no-follow-back.clj"))

(def followers-url
  ""
  "https://api.twitter.com/1.1/followers/list.json")

(def friends-url
  ""
  "https://api.twitter.com/1.1/friends/list.json")