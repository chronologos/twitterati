(ns twitter-followers.core
  (:require
    [oauth.client :as oauth]
    [environ.core :refer [env]]
    [clojure.data.json :as json]
    [clj-http.client :as client]
    [clojure.edn :as edn]
    [clojure.tools.logging :as log])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(def consumer-key
  (env :consumer-key))

(def consumer-secret
  (env :consumer-secret))

(def oauth-token
  (env :oauth-token))

(def oauth-secret
  (env :oauth-secret))


(def consumer (oauth/make-consumer consumer-key
                                   consumer-secret
                                   "https://api.twitter.com/oauth/request_token"
                                   "https://api.twitter.com/oauth/access_token"
                                   "https://api.twitter.com/oauth/authorize"
                                   :hmac-sha1))

(defn credentials [user-params] (oauth/credentials consumer
                                                   nil
                                                   nil
                                                   :GET
                                                   "https://api.twitter.com/1.1/followers/list.json"
                                                   user-params))

(defn http-fetch-single-cursor [cursor]
  (let [user-params {:cursor cursor
                     :screen_name "iantay_"
                     :skip_status "true"
                     :include_user_entities "false"}
        resp (try+ (client/get
                     "https://api.twitter.com/1.1/followers/list.json"
                     {:query-params  (merge (credentials user-params) user-params)
                      :debug         false
                      :cookie-policy :standard
                      })
                   (catch [:status 429] {:keys [request-time headers body]}
                     (log/error "429 - Too many requests" request-time headers) (throw+))
                   (catch Object _
                     (log/error (:throwable &throw-context) "unexpected error")
                     (throw+))
                   )
        body (-> resp
                 :body
                 json/read-str)
        next-cursor-str (get body "next_cursor_str")
        users1 (get body "users")
        users2 (map #(select-keys % ["followers_count" "screen_name"]) users1)]
    (hash-map :followers users2 :next-cursor next-cursor-str))
  )

(defn fetch-all [cursor res paginated-fetcher]
  (let [single-res (paginated-fetcher cursor)
        next-cursor (:next-cursor single-res)
        this-res (:followers single-res)
        merged (concat res this-res)]
    (if (some #(= next-cursor %) ["0" ""])
      merged
      (fetch-all next-cursor merged paginated-fetcher)
      )))

(defn -main []
  (let [res (fetch-all "-1" [] http-fetch-single-cursor)
        _ (spit "data.clj" (pr-str res))
        ;prev-res (read-string (slurp "data.clj"))
        ]
    (spit "data-pretty.clj"
          (binding [*out* (java.io.StringWriter.)]
            (clojure.pprint/print-table res)
            (.toString *out*)))
    ;;(let [prev-res (read-string (slurp "data.clj"))
    ;;      _ (println consumer-secret)
    ;;      sorted-res (sort-by #(get % "screen_name") prev-res)
    ;;      nl-res (interpose "\n" sorted-res)]
    ;;  (println nl-res)
    ;;  )
    ))





