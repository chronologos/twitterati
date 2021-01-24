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
    (io/make-parents names/no-follow-back-fpath)
    (spit names/no-follow-back-fpath (pr-str no-follow-back)) ;; friends who don't follow back
    (spit names/follower-names-fpath (pr-str curr-followers-names)) ;; names of current followers
    (spit names/data-fpath (pr-str followers')) ;; data of current followers
    (spit names/historical-followers-fpath (pr-str all-followers)) ;; names of all historical followers
    ;; aggregated count statistics
    (spit names/running-data-fpath (println-str
                                    "date: " (now)
                                    ", follower-count: " (count followers')
                                    ", follower-reach: " follower-total-reach
                                    ", all-time-followers: " (count  all-followers))
          :append (.exists (io/file names/running-data-fpath)))
    ;; diff (added removed same) over time
    (spit names/running-diff-fpath (println-str diff)
          :append (.exists (io/file names/running-diff-fpath)))
    ;; pretty-printed table of followers
    (spit names/follower-table-fpath
          (binding [*out* (java.io.StringWriter.)]
            (clojure.pprint/print-table followers')
            (.toString *out*)))))

(defn -main []
  (let [use-cache false ;; set to true to use cached Twitter responses rather than calling twitter/fetch-all
        historical-followers (try (read-string (slurp names/historical-followers-fpath))
                                  (catch java.io.FileNotFoundException _
                                    '()))
        prev-state (try (read-string (slurp names/state-fpath))
                        (catch java.io.FileNotFoundException _
                          {}))
        stored-next-cursor (:cursor prev-state "-1")
        partial-data (:partial-data prev-state [])
        stored-next-cursor-friends (:cursor-friends prev-state "-1")
        partial-data-friends (:partial-data-friends prev-state [])
        [current-followers-raw new-cursor] (if use-cache 
                                             [(read-string (slurp "./test/followers.clj")) "0"]
                                             (twitter/fetch-all names/followers-url stored-next-cursor partial-data twitter/http-fetch-single-cursor))
        [current-friends-raw new-cursor-friends]  (if use-cache 
                                                    [(read-string (slurp "./test/friends.clj")) "0"]
                                                    (twitter/fetch-all names/friends-url stored-next-cursor-friends partial-data-friends twitter/http-fetch-single-cursor))
        _ (when-not use-cache (spit "./test/followers.clj" (pr-str current-followers-raw)))
        _ (when-not use-cache (spit "./test/friends.clj" (pr-str current-friends-raw)))
        all-friends-fetched (= new-cursor-friends "0")
        all-followers-fetched (= new-cursor "0")
        all-data-fetched (and all-friends-fetched all-followers-fetched)]
    ; if all data was fetched, do processing.
    (when all-data-fetched (process-raw-data current-followers-raw historical-followers current-friends-raw))
    ; save current cursor and data state. This allows the next run to pick up from where we left off if not all data was fetched.
    (spit names/state-fpath (pr-str
                             {:cursor (if all-followers-fetched "-1" new-cursor)
                              :partial-data (if all-followers-fetched [] current-followers-raw)
                              :cursor-friends (if all-friends-fetched "-1" new-cursor-friends)
                              :partial-data-friends (if all-friends-fetched [] current-friends-raw)}))))