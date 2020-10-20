(ns stan
  (:require [cheshire.core :as json]
            [codesmith.logger :as log]))

(log/deflogger)

(comment

  (log/info-m "import message for {}, status {}" (log/kv :user "stan") 400)

  (log/error-e (IllegalStateException. "hello")
               nil)

  (log/info-c {:a 1 :b 2} "hello {} {} {} "
              :name "stan"
              :a 1
              :b 2)

  (log/info-c nil "stan")

  )