(ns twitter-followers.twitter
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [twitter-followers.names :as names]
   [clj-http.client :as client]
   [oauth.client :as oauth]
   [clojure.tools.logging :as log]
   [clojure.data.json :as json]
   [slingshot.slingshot :refer [try+ throw+]]))

(def consumer (oauth/make-consumer names/consumer-key
                                   names/consumer-secret
                                   "https://api.twitter.com/oauth/request_token"
                                   "https://api.twitter.com/oauth/access_token"
                                   "https://api.twitter.com/oauth/authorize"
                                   :hmac-sha1))

(defn credentials [user-params url] (oauth/credentials consumer
                                                       nil
                                                       nil
                                                       :GET
                                                       url
                                                       user-params))

(defn http-fetch-single-cursor
  "Gets a single page of twitter followers/friends for a given user."
  [url cursor]
  (log/info url " -- " cursor "\n")
  (let [user-params {:cursor                cursor
                     :count                 200 ; max - will fail if total exceeds 200*15 in a 15 min window
                     :screen_name           names/username
                     :skip_status           "true"
                     :include_user_entities "false"}
        resp (try+ (client/get url
                               {:query-params  (merge (credentials user-params url) user-params)
                                :debug         false
                                :cookie-policy :standard})
                   (catch [:status 429] #_{:clj-kondo/ignore [:unresolved-symbol]} _
                     (log/error "429 - Too many requests" user-params) (throw+))
                   (catch Object _
                     (log/error (:throwable #_{:clj-kondo/ignore [:unresolved-symbol]} &throw-context) "unexpected error")
                     (throw+)))
        ;; _ (log/debug  "resp:" resp "\n")
        bod (-> resp
                 :body
                 ) ;; read json but turn keys (string) into keyword
        body (json/read-str bod :key-fn keyword)
    ;; _ (log/debug  "body:" body "\n")
        next-cursor-str (:next_cursor_str body)
        users1 (:users body)
        users2 (map #(select-keys % [:followers_count :name :screen_name]) users1)
        _ (log/debug  "next cursor:" next-cursor-str "\n")
  ]
    (hash-map ::followers users2 ::next-cursor next-cursor-str)))

(s/fdef http-fetch-single-cursor :args (s/cat :url string? :cursor string?))
(stest/instrument `http-fetch-single-cursor)

(defn fetch-all
  "Gets all twitter followers for a given user. 
   Next-cursor == 0 -> fetch completed
               == -1 -> fetch failed on first call
               else fetch succeeded and needs to get next page"
  [url cursor res paginated-fetcher]
  (try+
   (let [single-res (paginated-fetcher url cursor)
         next-cursor (::next-cursor single-res)
         this-res (::followers single-res)
         merged (concat res this-res)]
     (if (some #(= next-cursor %) ["0" "" 0])
       [merged "0"]
       (fetch-all url next-cursor merged paginated-fetcher)))
   (catch [:status 429] _
     [res cursor])))

;; some testing with clojure.spec
(s/def ::followers string?)
(s/def ::next-cursor string?)
(s/def ::http-result (s/keys :req [::followers ::next-cursor]
                                   :opt [::skills]))
(s/valid? ::http-result {::followers "hi" ::next-cursor "hi"})

(s/def ::followers-count int?)
(s/def ::name string?)
(s/def ::screen_name string?)
(s/def ::twitter-user-data (s/keys :req-un [::followers-count ::name ::screen_name]))
(s/valid? ::twitter-user-data {:followers-count 1 :name "john" :screen_name "hello"})