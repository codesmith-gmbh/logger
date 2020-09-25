(ns ^{:author "Stanislas Nanchen"
      :doc    "Codesmith Logger is a simple wrapper on logback (slf4j) and net.logstash.logback.

               To use the library, it is necessary to call the macro `deflogger`
               before any logging macro is called."}
  codesmith.logger.core
  (:require [cheshire.core]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [org.slf4j LoggerFactory Logger]
           [codesmith.logger ClojureMapMarker]))

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

(defn coerce-string [arg]
  (if (instance? String arg)
    arg
    `(str ~arg)))

(defn coerce-object [arg]
  (if (some #(instance? % arg) [Long Double Integer])
    `(identity ~arg)
    arg))

(defn coerce-throwable ^Throwable [e]
  (if (instance? Throwable e)
    e
    (ex-info (str e) {})))

(defn ^"[Ljava.lang.Object;" into-object-array [& args]
  (into-array Object args))

(defmacro log-c
  ([method ctx]
   (if (resolve '⠇⠕⠶⠻)
     `(. ~'⠇⠕⠶⠻
         (~method (ClojureMapMarker. ~ctx) ""))
     (throw (IllegalStateException. "(deflogger) has not been called"))))
  ([method ctx msg]
   (if (resolve '⠇⠕⠶⠻)
     `(. ~'⠇⠕⠶⠻
         (~method
           (ClojureMapMarker. ~ctx)
           ~(coerce-string msg)))
     (throw (IllegalStateException. "(deflogger) has not been called"))))
  ([method ctx msg & args]
   (if (resolve '⠇⠕⠶⠻)
     (case (count args)
       0 `(. ~'⠇⠕⠶⠻
             (~method
               (ClojureMapMarker. ~ctx)
               ~(coerce-string msg)))
       1 `(. ~'⠇⠕⠶⠻
             (~method
               (ClojureMapMarker. ~ctx)
               ~(coerce-string msg)
               ~(coerce-object (first args))))
       2 `(. ~'⠇⠕⠶⠻
             (~method
               (ClojureMapMarker. ~ctx)
               ~(coerce-string msg)
               ~(coerce-object (first args))
               ~(coerce-object (second args))))
       `(. ~'⠇⠕⠶⠻
           (~method
             (ClojureMapMarker. ~ctx)
             ~(coerce-string msg)
             (into-object-array ~@args))))
     (throw (IllegalStateException. "(deflogger) has not been called")))))

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
    (throw (IllegalStateException. "(deflogger) has not been called"))))

(defmacro log-e
  ([method e]
   `(let [e#   (coerce-throwable ~e)
          msg# (.getMessage e#)]
      (log-e ~method e# msg#)))
  ([method e msg]
   (if (resolve '⠇⠕⠶⠻)
     `(let [e#     (coerce-throwable ~e)
            e-ctx# (ex-data e#)]
        (if e-ctx#
          (. ~'⠇⠕⠶⠻
             (~method
               (ClojureMapMarker. e-ctx#)
               ~(coerce-string msg)
               e#))
          (. ~'⠇⠕⠶⠻
             (~method
               ~(coerce-string msg)
               e#))))
     (throw (IllegalStateException. "(deflogger) has not been called"))))
  ([method e ctx msg]
   (if (resolve '⠇⠕⠶⠻)
     `(let [e#     (coerce-throwable ~e)
            e-ctx# (ex-data e#)
            ctx#   ~ctx]
        (if e-ctx#
          (. ~'⠇⠕⠶⠻
             (~method
               (ClojureMapMarker. (into e-ctx# ctx#))
               ~(coerce-string msg)
               ^Throwable e#))
          (. ~'⠇⠕⠶⠻
             (~method
               (ClojureMapMarker. ctx#)
               ~(coerce-string msg)
               ^Throwable e#))))
     (throw (IllegalStateException. "(deflogger) has not been called")))))

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
