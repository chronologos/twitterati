(ns twitter-followers.core-test
  (:require [clojure.test :refer :all]
             [twitter-followers.core :as tf]))

(def followers-1 [{"followers_count" "100" "screen_name" "a"}
                {"followers_count" "200" "screen_name" "b"}
                {"followers_count" "300" "screen_name" "c"}])

(def followers-2 [{"followers_count" "500" "screen_name" "aa"}
                {"followers_count" "600" "screen_name" "bb"}
                {"followers_count" "700" "screen_name" "cc"}])

(def follower-page-1 {:followers followers-1 :next-cursor "a"})
(def follower-page-2 {:followers followers-2 :next-cursor "0"})

(def fake-fetcher-calls (atom 0))
(defn fake-fetcher [_]
  (swap! fake-fetcher-calls inc)
  (if (= @fake-fetcher-calls 1) follower-page-1 follower-page-2)
  )

(deftest test-pagination
  (is (=
        (tf/fetch-all "-1" [] fake-fetcher)
        (concat followers-1 followers-2))))