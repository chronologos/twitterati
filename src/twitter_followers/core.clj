(ns twitter-followers.core
  (:require
   [twitter-followers.names :as names]
   [twitter-followers.twitter :as twitter]
   [clojure.data]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [clojure.pprint])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(defn now [] (.format (SimpleDateFormat. "MM/dd/yyyy") (new Date)))

(defn distinct-by [f coll]
  (let [groups (group-by f coll)]
    (map #(first (groups %)) (distinct (map f coll)))))

(defn mark-mutuals [followers friends]
  (map #(if (contains? friends (:screen_name %))
          (conj % {:mutual true})
          (conj % {:mutual false})) followers))

;; Friends -> people you follow
;; Followers -> people who follow you
;; raw data can contain duplicates due to pagination
(defn process-raw-data
  ""
  [followers-raw historical-followers friends-raw]
  (let [; current-followers-raw  (read-string (slurp "data.clj"))

        friends-set (->> (map :screen_name friends-raw) set)
        followers-set (->> (map :screen_name followers-raw) set)

        friends-by-followers  (sort-by :followers_count (distinct-by :screen_name friends-raw))
        ; some sanity prints
        _ (log/info "curr followers: " followers-set "\n")
        _ (log/info "curr friends: " friends-set "\n")
        _ (log/info "curr followers (raw count): " (count followers-raw) "\n")
        _ (log/info "curr friends (raw count): " (count friends-raw) "\n")
        _ (log/info "curr followers count: " (count followers-set) "\n")
        _ (log/info "curr friends count: " (count friends-by-followers) "\n")
        _ (log/info "curr friends set count (should be same as above): " (count friends-set) "\n")

        followers (sort-by :screen_name (distinct-by :screen_name followers-raw))
        followers'  (mark-mutuals followers friends-set)

        no-follow-back (remove #(contains? followers-set (:screen_name %)) friends-by-followers)
        _ (log/info "curr friends who don't follow back: " (count no-follow-back) "\n")

        curr-followers-names (mapv :screen_name followers')
        [curr-only hist-only both :as diff] (clojure.data/diff (set (map :screen_name followers')) (set  historical-followers))
        _ (log/info "co: " curr-only " ho: " hist-only " both: " both "\n")
        all-followers (remove nil? (distinct (sort (concat curr-only historical-followers))))
        follower-total-reach (->> followers'
                                  (map :followers_count)
                                  (reduce +))]
    ;; after processing, write to outputs
    (spit names/no-follow-back-filename (pr-str no-follow-back)) ;; friends who don't follow back
    (spit names/data-filename-name-only (pr-str curr-followers-names)) ;; names of current followers
    (spit names/data-filename (pr-str followers')) ;; data of current followers
    (spit names/data-filename-all-historical-names (pr-str all-followers)) ;; names of all historical followers
    ;; aggregated count statistics
    (spit names/running-data-filename (println-str
                                       "date: " (now)
                                       ", follower-count: " (count followers')
                                       ", follower-reach: " follower-total-reach
                                       ", all-time-followers: " (count  all-followers))
          :append (.exists (io/file names/running-data-filename)))
    ;; diff (added removed same) over time
    (spit names/running-diff-filename (println-str diff)
          :append (.exists (io/file names/running-diff-filename)))
    ;; pretty-printed table of followers
    (spit names/follower-table-filename
          (binding [*out* (java.io.StringWriter.)]
            (clojure.pprint/print-table followers')
            (.toString *out*)))))

(defn -main []
  (let [historical-followers (read-string (slurp names/data-filename-all-historical-names))
        prev-state (read-string (slurp names/state-filename))
        stored-next-cursor (:cursor prev-state "-1")
        partial-data (:partial-data prev-state [])
        stored-next-cursor-friends (:cursor-friends prev-state "-1")
        partial-data-friends (:partial-data-friends prev-state [])
        [current-followers-raw new-cursor] (twitter/fetch-all names/followers-url stored-next-cursor partial-data twitter/http-fetch-single-cursor)
        [current-friends-raw new-cursor-friends]  (twitter/fetch-all names/friends-url stored-next-cursor-friends partial-data-friends twitter/http-fetch-single-cursor)
        all-friends-fetched (= new-cursor-friends "0")
        all-followers-fetched (= new-cursor "0")
        all-data-fetched (and all-friends-fetched all-followers-fetched)]
    ; if all data was fetched, do processing.
    (when all-data-fetched (process-raw-data current-followers-raw historical-followers current-friends-raw))
    ; save current cursor and data state. This allows the next run to pick up from where we left off if not all data was fetched.
    (spit names/state-filename (pr-str
                                {:cursor (if all-followers-fetched "-1" new-cursor)
                                 :partial-data (if all-followers-fetched [] current-followers-raw)
                                 :cursor-friends (if all-friends-fetched "-1" new-cursor-friends)
                                 :partial-data-friends (if all-friends-fetched [] current-friends-raw)}))))