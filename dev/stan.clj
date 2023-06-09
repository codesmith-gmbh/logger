(ns stan
  (:require [ch.codesmith.logger :as log]
            [jsonista.core :as json]
            [criterium.core :as c])
  (:import (clojure.lang AFn AFunction MultiFn)
           (java.lang.invoke MethodHandles)
           (javax.lang.model.type ArrayType)))

(log/deflogger)

(defmulti test-multifn)

(defn field-getter [^Class class ^String name]
  (let [name-field (.getDeclaredField class name)]
    (when-not (.isAccessible name-field)
      (.setAccessible name-field true))
    (let [mh (.asSpreader
               (.unreflectGetter (MethodHandles/lookup) name-field)
               (Class/forName (str "[L" (.getName class) ";"))
               1)]
      (fn [multi-fn]
        (let [args (make-array MultiFn 1)]
          (aset args 0 multi-fn)
          (.invoke mh args))))))



(defn multifn-to-string-fn []
  (let [class-name (.getName MultiFn)]
    (try
      (let [name-getter (field-getter MultiFn "name")]
        #(str class-name " " (name-getter %)))
      (catch Exception e
        (constantly class-name)))))

(comment

  (let [getter (field-getter2 MultiFn "name")]
    (c/bench (getter test-multifn)))

  (.getName MultiFn)

  (doto (make-array Object 1)
    (aset 0 1))

  ((multifn-to-string-fn) test-multifn)

  (log/info-m "import message for {}, status {}" (log/kv :user "stan") 400)

  (log/error-e (IllegalStateException. "hello")
               nil)

  (log/info-c {:a 1} "hello {} {} {} "
              :name "stan"
              :a 1
              :b 2)

  (let [field (.getDeclaredField MultiFn "name")]
    (.setAccessible field true)
    (.get field test-multifn))



  (.get (.getDeclaredField MultiFn "name") test-multifn)

  (log/info-c nil "stan")

  (log/raw-json {:a test-multifn})

  (instance? AFunction {})
  (meta test-multifn)

  (.getName (class +))

  (meta #'+)

  )