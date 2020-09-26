(ns ^{:author "Stanislas Nanchen"
      :doc    "Codesmith Logger is a simple wrapper on logback (slf4j) and net.logstash.logback.

               To use the library, it is necessary to call the macro `deflogger`
               before any logging macro is called."}
  codesmith.logger.core
  (:require [cheshire.core]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [cheshire.core :as json])
  (:import [org.slf4j LoggerFactory Logger Marker]
           [net.logstash.logback.marker RawJsonAppendingMarker]
           [clojure.lang IDeref]))

;; # Configuration

(def default-context-logging-key "context")

(def context-logging-key default-context-logging-key)

(defn set-context-logging-key! [logging-key]
  (alter-var-root #'context-logging-key (constantly logging-key)))

(defn default-context-pre-logging-transformation [ctx]
  (persistent!
    (reduce-kv
      (fn [acc k v]
        (assoc! acc k (if (instance? IDeref v)
                        @v
                        v)))
      (transient {})
      ctx)))

(def context-pre-logging-transformation default-context-pre-logging-transformation)

(defn set-context-pre-logging-transformation! [tranformation]
  (alter-var-root #'context-pre-logging-transformation (constantly tranformation)))

;; # Logger definition

;; Install a var called `⠇⠕⠶⠻` in the namespace from which it is called.
;; The name is logger in braille-2 notation."
(defmacro deflogger []
  `(defonce ~(vary-meta '⠇⠕⠶⠻ assoc :tag Logger) (LoggerFactory/getLogger ~(.toString *ns*))))

;; # Logging macros
;;
;; In general, you should use the `info`, `warn`, etc variants 
;; as the `log` macros are considered low-level.
;;
;; For each log level, we have 3 macros with slight different arities and behavior:
;; 1. suffix `-c`:  3 arities
;;    i.   1 arg -> expects an exception created with `ex-info` and will use the data as context.
;;    ii.  2 args -> expects an exception created with `ex-info` and will use the data as context.
;;    iii. n args -> ctx, msg and arg : e is exception, if raise with `ex-info`, 
;;                   contributes to the ctx; msg with formatting
;; 2. suffix `-m`: formatting of message no context
;; 3. suffix `-e`: for exceptions with and without ctx works best with an exception 
;;                 created with `ex-info` : it will use the data as context.
;;
;; The spy macro allows to log a value being evaluated (as well as the original expression) 
;; and return the evaluated value. The first argument is the keyword of the level 
;; (:info, :warn, etc...)

(deflogger)

(defn coerce-string [arg]
  (if (instance? String arg)
    arg
    `(str ~arg)))

(defn coerce-object [arg]
  (if (instance? Long arg)
    `(Long/valueOf ~arg)
    (if (instance? Double arg)
      `(Double/valueOf ~arg)
      arg)))

(defn coerce-throwable ^Throwable [e]
  (if (instance? Throwable e)
    e
    (let [e-str (str e)]
      (.warn ⠇⠕⠶⠻ "Value {} is not a throwable; wrapping in ex-info" e-str)
      (ex-info e-str {}))))

(defn ^"[Ljava.lang.Object;" into-object-array [& args]
  (into-array Object args))

(defn ^Marker ctx-marker [ctx]
  (let [ctx   (context-pre-logging-transformation ctx)
        value (try
                (json/generate-string ctx)
                (catch Exception e
                  (.warn ⠇⠕⠶⠻ "Serialization error" ^Exception e)
                  (json/generate-string (pr-str ctx))))]
    (RawJsonAppendingMarker. context-logging-key value)))

(defn level-pred [method]
  (let [method-name (name method)]
    (symbol
      (str
        "is"
        (str/upper-case (subs method-name 0 1))
        (subs method-name 1)
        "Enabled"))))

(defn throw-logger-missing-exception []
  (throw (IllegalStateException. (str "(deflogger) has not been called in current namespace `" *ns* "`"))))

(defmacro log-c
  ([method ctx]
   (if (resolve '⠇⠕⠶⠻)
     `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
        (. ~'⠇⠕⠶⠻
           (~method (ctx-marker ~ctx) "")))
     (throw-logger-missing-exception)))
  ([method ctx msg]
   (if (resolve '⠇⠕⠶⠻)
     `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
        (. ~'⠇⠕⠶⠻
           (~method
             (ctx-marker ~ctx)
             ~(coerce-string msg))))
     (throw-logger-missing-exception)))
  ([method ctx msg & args]
   (if (resolve '⠇⠕⠶⠻)
     (case (count args)
       0 `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
            (. ~'⠇⠕⠶⠻
               (~method
                 (ctx-marker ~ctx)
                 ~(coerce-string msg))))
       1 `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
            (. ~'⠇⠕⠶⠻
               (~method
                 (ctx-marker ~ctx)
                 ~(coerce-string msg)
                 ~(coerce-object (first args)))))
       2 `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
            (. ~'⠇⠕⠶⠻
               (~method
                 (ctx-marker ~ctx)
                 ~(coerce-string msg)
                 ~(coerce-object (first args))
                 ~(coerce-object (second args)))))
       `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
          (. ~'⠇⠕⠶⠻
             (~method
               (ctx-marker ~ctx)
               ~(coerce-string msg)
               (into-object-array ~@args)))))
     (throw-logger-missing-exception))))

(defmacro log-m [method msg & args]
  (if (resolve '⠇⠕⠶⠻)
    (case (count args)
      0 `(. ~'⠇⠕⠶⠻
            (~method
              ~msg))
      1 `(. ~'⠇⠕⠶⠻
            (~method
              ~(coerce-string msg)
              ~(coerce-object (first args))))
      2 `(. ~'⠇⠕⠶⠻
            (~method
              ~(coerce-string msg)
              ~(coerce-object (first args))
              ~(coerce-object (second args))))
      `(. ~'⠇⠕⠶⠻
          (~method
            ~(coerce-string msg)
            (into-object-array ~@args))))
    (throw-logger-missing-exception)))

(defmacro log-e
  ([method e]
   `(let [e#   (coerce-throwable ~e)
          msg# (.getMessage e#)]
      (log-e ~method e# msg#)))
  ([method e msg]
   (if (resolve '⠇⠕⠶⠻)
     `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
        (let [e#     (coerce-throwable ~e)
              e-ctx# (ex-data e#)]
          (if e-ctx#
            (. ~'⠇⠕⠶⠻
               (~method
                 (ctx-marker e-ctx#)
                 ~(coerce-string msg)
                 e#))
            (. ~'⠇⠕⠶⠻
               (~method
                 ~(coerce-string msg)
                 e#)))))
     (throw-logger-missing-exception)))
  ([method e ctx msg]
   (if (resolve '⠇⠕⠶⠻)
     `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
        (let [e#     (coerce-throwable ~e)
              e-ctx# (ex-data e#)
              ctx#   ~ctx]
          (if e-ctx#
            (. ~'⠇⠕⠶⠻
               (~method
                 (ctx-marker (into e-ctx# ctx#))
                 ~(coerce-string msg)
                 ^Throwable e#))
            (. ~'⠇⠕⠶⠻
               (~method
                 (ctx-marker ctx#)
                 ~(coerce-string msg)
                 ^Throwable e#)))))
     (throw-logger-missing-exception))))

(defmacro spy
  ([val]
   `(spy :debug ~val))
  ([level val]
   (let [method (symbol (name level))]
     `(let [val# ~val]
        (log-c ~method {:expression (delay
                                      (str/trim (with-out-str
                                                  (pp/with-pprint-dispatch
                                                    pp/code-dispatch
                                                    (pp/pprint '~val)))))
                        :value      val#}
               "spy")
        val#))))


(defmacro trace-c
  ([ctx]
   `(log-c ~'trace ~ctx))
  ([ctx msg]
   `(log-c ~'trace ~ctx ~msg))
  ([ctx msg & args]
   `(log-c ~'trace ~ctx ~msg ~@args)))

(defmacro trace-m [msg & args]
  `(log-m ~'trace ~msg ~@args))

(defmacro trace-e
  ([e]
   `(log-e ~'trace ~e))
  ([e msg]
   `(log-e ~'trace ~e ~msg))
  ([e ctx msg]
   `(log-e ~'trace ~e ~ctx ~msg)))


(defmacro debug-c
  ([ctx]
   `(log-c ~'debug ~ctx))
  ([ctx msg]
   `(log-c ~'debug ~ctx ~msg))
  ([ctx msg & args]
   `(log-c ~'debug ~ctx ~msg ~@args)))

(defmacro debug-m [msg & args]
  `(log-m ~'debug ~msg ~@args))

(defmacro debug-e
  ([e]
   `(log-e ~'debug ~e))
  ([e msg]
   `(log-e ~'debug ~e ~msg))
  ([e ctx msg]
   `(log-e ~'debug ~e ~ctx ~msg)))


(defmacro info-c
  ([ctx]
   `(log-c ~'info ~ctx))
  ([ctx msg]
   `(log-c ~'info ~ctx ~msg))
  ([ctx msg & args]
   `(log-c ~'info ~ctx ~msg ~@args)))

(defmacro info-m [msg & args]
  `(log-m ~'info ~msg ~@args))

(defmacro info-e
  ([e]
   `(log-e ~'info ~e))
  ([e msg]
   `(log-e ~'info ~e ~msg))
  ([e ctx msg]
   `(log-e ~'info ~e ~ctx ~msg)))


(defmacro warn-c
  ([ctx]
   `(log-c ~'warn ~ctx))
  ([ctx msg]
   `(log-c ~'warn ~ctx ~msg))
  ([ctx msg & args]
   `(log-c ~'warn ~ctx ~msg ~@args)))

(defmacro warn-m [msg & args]
  `(log-m ~'warn ~msg ~@args))

(defmacro warn-e
  ([e]
   `(log-e ~'warn ~e))
  ([e msg]
   `(log-e ~'warn ~e ~msg))
  ([e ctx msg]
   `(log-e ~'warn ~e ~ctx ~msg)))


(defmacro error-c
  ([ctx]
   `(log-c ~'error ~ctx))
  ([ctx msg]
   `(log-c ~'error ~ctx ~msg))
  ([ctx msg & args]
   `(log-c ~'error ~ctx ~msg ~@args)))

(defmacro error-m [msg & args]
  `(log-m ~'error ~msg ~@args))

(defmacro error-e
  ([e]
   `(log-e ~'error ~e))
  ([e msg]
   `(log-e ~'error ~e ~msg))
  ([e ctx msg]
   `(log-e ~'error ~e ~ctx ~msg)))
