(ns com.kaicode.fb
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs.core.async :refer [<! >! put! chan]]
            [com.kaicode.tily :as tily]
            [clojure.walk :as w]
            [clojure.string :as s]))

(defn get-profile [response]
  (let [fields ["id" "first_name" "last_name" "gender" "email" "is_verified" "verified"]
        c (chan 1)]
    (.. js/FB (api "/me" (clj->js {:fields (s/join "," fields)})
                   (fn [result]
                     (let [user-profile (-> result js->clj
                                            (select-keys fields)
                                            (w/keywordize-keys))]
                       (go (>! c user-profile))))))
    c))

(defn init [{:keys [fb-id on-connected on-not-connected] :as params}]
  (aset js/window "fbAsyncInit" (fn []
                                  (.. js/FB (init (clj->js {:appId fb-id
                                                            :cookie true
                                                            :xfbml true
                                                            :version "v2.8"})))
                                  (.. js/FB (getLoginStatus (fn [response]
                                                              (let [status (aget response "status")]
                                                                (if (= status "connected")
                                                                  (on-connected response)
                                                                  (on-not-connected response)))))))))

(defn login [{:keys [on-connected on-not-connected]}]
  (.. js/FB (login (fn [response]
                     (let [auth-response (aget response "authResponse")]
                       (when auth-response
                         (on-connected response))))
                   (clj->js {:scope "public_profile,email"}))))
