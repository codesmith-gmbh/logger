(ns ch.codesmith.logger
  (:refer-clojure :exclude [assoc])
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [jsonista.core :as json])
  (:import (clojure.lang AFunction MultiFn RT)
           (com.fasterxml.jackson.core JsonGenerator)
           (java.util Collection)
           (net.logstash.logback.argument StructuredArguments)
           (net.logstash.logback.marker Markers RawJsonAppendingMarker)
           (org.slf4j Logger LoggerFactory)))

;; # Logger definition

(defmacro deflogger
  "Creates a var named `⠇⠕⠶⠻` in the current namespace `*ns*` to contain a [[org.slf4j.Logger]]
  The name is \"logger\" written in braille-2 notation."
  []
  `(defonce ~(vary-meta '⠇⠕⠶⠻ clojure.core/assoc :tag Logger) (LoggerFactory/getLogger ~(.toString *ns*))))

(deflogger)

;; # Configuration

(def default-context-logging-key "context")

(def context-logging-key
  "The key under which the [[log-c]], [[log-m]] and [[log-e]] macros put the context in the logstash JSON.
  By default, it is \"context\"."
  default-context-logging-key)

(defn set-context-logging-key!
  "Configuration function to set the [[context-logging-key]]."
  [logging-key]
  (alter-var-root #'context-logging-key (constantly logging-key)))

(def default-ex-data-logging-key "exdata")

(def ex-data-logging-key
  "The key under with the [[log-e]] macros put the `ex-data` from an exception in the logstash JSON.
  By default, it is \"exdata\"."
  default-ex-data-logging-key)

(defn set-ex-data-logging-key!
  "Configuration function to set the [[ex-data-logging-key]]."
  [logging-key]
  (alter-var-root #'ex-data-logging-key (constantly logging-key)))

(def default-context-pre-logging-transformation identity)

(def context-pre-logging-transformation
  "A transformation function that is applied to the context before logging.
  By default, it is the identity (no transformation)."
  default-context-pre-logging-transformation)

(defn set-context-pre-logging-transformation!
  "Configuration function to set the [[context-pre-logging-transformation]]"
  [tranformation]
  (alter-var-root #'context-pre-logging-transformation (constantly tranformation)))

(def default-version-file-path "/app/version.edn")

(def version-file-path
  "A path (as string) where a edn file with the version of the program is located.
  The edn file must be a map with the key `:version`.
  By default, it is the location where the [anvil](https://github.com/codesmith-gmbh/anvil)
  library creates a version edn file when building docker artifacts."
  default-version-file-path)

(defn set-version-file-path!
  "Configuration function to set the [[version-file-path]]"
  [path]
  (alter-var-root #'version-file-path (constantly path)))

(defn to-string-encoder [value ^JsonGenerator gen]
  (.writeString gen (str value)))

(defn to-class-name-encoder [value ^JsonGenerator gen]
  (to-string-encoder (if value (.getName (class value)) "")
                     gen))

(defn field-getter [^Class class ^String name]
  (let [name-field (.getDeclaredField class name)]
    (.setAccessible name-field true)
    (fn [multi-fn]
      (.get name-field multi-fn))))

(def multifn-to-string
  (let [class-name (.getName MultiFn)]
    (try
      (let [name-getter (field-getter MultiFn "name")]
        #(str class-name "/" (name-getter %)))
      (catch Exception e
        (.debug #_{:clj-kondo/ignore [:unresolved-symbol]} ⠇⠕⠶⠻ "MultiFn#name is not accessible.s" ^Exception e)
        (constantly class-name)))))

(defn multi-fn-encoder [^MultiFn value ^JsonGenerator gen]
  (.writeString gen ^String (multifn-to-string value)))

(def default-jsonista-object-mapper
  (json/object-mapper {:encoders {MultiFn   multi-fn-encoder
                                  AFunction to-class-name-encoder}}))

(def jsonista-object-mapper
  "The object mapper used by jsonista for the serialization in raw json of the
  context markers created by this library."
  default-jsonista-object-mapper)

(defn set-jsonista-object-mapper
  "Configuration function to ste the [[jsonista-object-mapper]]"
  [object-mapper]
  (alter-var-root #'jsonista-object-mapper (constantly object-mapper)))

;; # Utility functions and macros.

(defn raw-json [value]
  (try
    (json/write-value-as-string value jsonista-object-mapper)
    (catch Exception e
      (.warn #_{:clj-kondo/ignore [:unresolved-symbol]} ⠇⠕⠶⠻ "Serialization error" ^Exception e)
      (json/write-value-as-string (pr-str value) jsonista-object-mapper))))

(defmacro coerce-string
  "Coerce, at compile time, the argument to be a String."
  [arg]
  (if (instance? String arg)
    arg
    `(str ~arg)))

(defmacro box "Ensures at compile time that the argument is an Object.
  This macro is necessary to avoid reflection warning from numeric constants that are emitted unboxed by the compiler."
  [arg]
  `(RT/box ~arg))

(defn fullname [k]
  (if (string? k)
    k
    (if-let [namespace (namespace k)]
      (.toString
        (doto (StringBuilder.)
          (.append namespace)
          (.append "/")
          (.append (name k))))
      (name k))))

(defn kv [k v]
  (StructuredArguments/raw
    (fullname k)
    (raw-json v)))

(defmacro compile-to-struct-args
  "Compile code to create a java array with the given arguments"
  [& args]
  (let [struct-args (partition 2 args)
        assignments (map-indexed (fn [i [k v]]
                                   `(aset ~i (kv ~k ~v)))
                                 struct-args)]
    `(doto (object-array ~(count struct-args))
       ~@assignments)))

(defmacro compile-to-array
  "Compile code to create a java array with the given arguments"
  [& args]
  (let [assignments (map-indexed (fn [i arg]
                                   `(aset ~i (box ~arg)))
                                 args)]
    `(doto (object-array ~(count args))
       ~@assignments)))

(defn ensure-throwable
  "Ensures, at runtime, that the argument is a throwable.
  If the argument is not throwable, its string representation is embedded in an `ex-info`."
  ^Throwable [e]
  (if (instance? Throwable e)
    e
    (let [e-str (str e)]
      (.warn ⠇⠕⠶⠻ "Value {} is not a throwable; wrapping in ex-info" e-str)
      (ex-info e-str {}))))

(defn level-pred
  "For the given `method` symbol, compute the corresponding `isXXXEnabled` method symbol."
  [method]
  (let [method-name (name method)]
    (symbol
      (str
        "is"
        (str/upper-case (subs method-name 0 1))
        (subs method-name 1)
        "Enabled"))))

(defn throw-logger-missing-exception
  "Throw an [[IllegalStateException]] to signal that the `deflogger` macro has not been called properly."
  []
  (throw (IllegalStateException. (str "(deflogger) has not been called in current namespace `" *ns* "`"))))

;; # Context and Marker

(defn aggregate-markers ^org.slf4j.Marker [^Collection args]
  (Markers/aggregate args))

(defprotocol ToContextMarker
  (marker ^org.slf4j.Marker [self logging-key])
  (ctx-marker ^org.slf4j.Marker [self]))

(extend-type nil
  ToContextMarker
  (marker [_ _]
    (Markers/empty))
  (ctx-marker [_]
    (Markers/empty)))

(extend-type Object
  ToContextMarker
  (marker [self logging-key]
    ; Creates a [[RawJsonAppendingMarker]] to include `ctx` in the JSON map produced by the logstash encoder.
    ; This function attempts first to transform `ctx` in JSON with cheshire; if it fails, it is transformed
    ; as JSON string with [[pr-str]].
    (let [ctx   (context-pre-logging-transformation self)
          value (raw-json ctx)]
      (RawJsonAppendingMarker. logging-key value)))
  (ctx-marker [self]
    (marker self context-logging-key)))

(defrecord Context [^org.slf4j.Marker mrkr ctx]
  ToContextMarker
  (marker [_ logging-key]
    (if (= logging-key context-logging-key)
      mrkr
      (marker ctx logging-key)))
  (ctx-marker [_] mrkr))

(defn ctx
  "Wraps the supplied map in a record containing a prerendered Marker; this allows to use a
  relatively static context without rerendering it as json on every log entry."
  [m]
  (->Context (ctx-marker m) m))

(defn assoc
  "Assoc the given key/values a prerendered context."
  ([{:keys [m]} k v]
   (ctx (clojure.core/assoc m k v)))
  ([{:keys [m]} k v & kvs]
   (ctx (apply clojure.core/assoc m k v kvs))))

;; # Logging macros

(defmacro log-c
  "Logging macro to output the context map `ctx` in the JSON string generated by the logstash encoder.
  Variants allow to pass a message as slf4j format string with 2*n structured arguments.
  The argument `method` is the symbol of the log method to call on the [[Logger]] object. Typically,
  the level macros (`trace-c`, `debug-c`, etc.) are used instead of this macro.
  The macro generates code that verifies that the corresponding log level is enabled."
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
             (coerce-string ~msg))))
     (throw-logger-missing-exception)))
  ([method ctx msg & struct-args]
   (when (odd? (count struct-args))
     (throw (IllegalArgumentException. "log-c expects even of arguments after the message (key value pairs), found odd number")))
   (if (resolve '⠇⠕⠶⠻)
     (case (count struct-args)
       0 `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
            (. ~'⠇⠕⠶⠻
               (~method
                 (ctx-marker ~ctx)
                 (coerce-string ~msg))))
       2 (let [[k v] struct-args]
           `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
              (. ~'⠇⠕⠶⠻
                 (~method
                   (ctx-marker ~ctx)
                   (coerce-string ~msg)
                   (kv ~k ~v)))))
       4 (let [[k1 v1 k2 v2] struct-args]
           `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
              (. ~'⠇⠕⠶⠻
                 (~method
                   (ctx-marker ~ctx)
                   (coerce-string ~msg)
                   (kv ~k1 ~v1)
                   (kv ~k2 ~v2)))))
       `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
          (. ~'⠇⠕⠶⠻
             (~method
               (ctx-marker ~ctx)
               (coerce-string ~msg)
               (compile-to-struct-args ~@struct-args)))))
     (throw-logger-missing-exception))))

(defmacro log-s
  "Logging macro for a logging message `msg` with structured arguments
  Variants allow to pass a message as slf4j format string with 2*n structured arguments.
  The argument `method` is the symbol of the log method to call on the [[Logger]] object. Typically,
  the level macros (`trace-s`, `debug-s, etc.) are used instead of this macro.
  The macro generates code that verifies that the corresponding log level is enabled."
  [method msg & struct-args]
  (when (odd? (count struct-args))
    (throw (IllegalArgumentException. "log-s expects even of arguments after the message (key value pairs), found odd number")))
  (if (resolve '⠇⠕⠶⠻)
    (case (count struct-args)
      0 `(. ~'⠇⠕⠶⠻
            (~method
              (coerce-string ~msg)))
      2 (let [[k v] struct-args]
          `(. ~'⠇⠕⠶⠻
              (~method
                (coerce-string ~msg)
                (kv ~k ~v))))
      4 (let [[k1 v1 k2 v2] struct-args]
          `(. ~'⠇⠕⠶⠻
              (~method
                (coerce-string ~msg)
                (kv ~k1 ~v1)
                (kv ~k2 ~v2))))
      `(. ~'⠇⠕⠶⠻
          (~method
            (coerce-string ~msg)
            (compile-to-struct-args ~@struct-args))))
    (throw-logger-missing-exception)))

(defmacro log-m
  "Logging macro for a simple logging message `msg`.
  Variants allow to pass a message as slf4j format string with n arguments.
  The argument `method` is the symbol of the log method to call on the [[Logger]] object. Typically,
  the level macros (`trace-m`, `debug-m`, etc.) are used instead of this macro."
  [method msg & args]
  (if (resolve '⠇⠕⠶⠻)
    (case (count args)
      0 `(. ~'⠇⠕⠶⠻
            (~method
              (coerce-string ~msg)))
      1 `(. ~'⠇⠕⠶⠻
            (~method
              (coerce-string ~msg)
              (box ~(first args))))
      2 `(. ~'⠇⠕⠶⠻
            (~method
              (coerce-string ~msg)
              (box ~(first args))
              (box ~(second args))))
      `(. ~'⠇⠕⠶⠻
          (~method
            (coerce-string ~msg)
            (compile-to-array ~@args))))
    (throw-logger-missing-exception)))

(defmacro log-e
  "Logging macro for logging a [[Throwable]] using the dedicated logging methods for errors.
  Variants allow to pass a context `ctx` and an explicit message `msg`. If no `msg` is
  provided, the message of the [[Throwable]] is use instead. If `ex-data` of the exception is defined,
  it is included as a Marker under the key [[ex-data-logging-key]].
  The argument `method` is the symbol of the log method to call on the [[Logger]] object. Typically,
  the level macros (`trace-e`, `debug-e`, etc.) are used instead of this macro.
  The macro generates code that verifies that the corresponding log level is enabled."
  ([method e]
   `(log-e ~method ~e nil nil))
  ([method e ctx]
   `(log-e ~method ~e ~ctx nil))
  ([method e ctx msg]
   (if (resolve '⠇⠕⠶⠻)
     `(if (. ~'⠇⠕⠶⠻ ~(level-pred method))
        (let [e#            (ensure-throwable ~e)
              e-ctx-marker# (marker (ex-data e#) ex-data-logging-key)
              ctx-marker#   (ctx-marker ~ctx)
              msg#          (or ~msg (.getMessage e#))]
          (. ~'⠇⠕⠶⠻
             (~method
               (aggregate-markers [e-ctx-marker# ctx-marker#])
               (coerce-string msg#)
               ^Throwable e#))))
     (throw-logger-missing-exception))))

(defmacro trace-c
  "Uses `loc-c` on trace level."
  ([ctx]
   `(log-c ~'trace ~ctx))
  ([ctx msg]
   `(log-c ~'trace ~ctx ~msg))
  ([ctx msg & struct-args]
   `(log-c ~'trace ~ctx ~msg ~@struct-args)))

(defmacro trace-s
  "Uses `log-s` on trace level."
  [msg & struct-args]
  `(log-s ~'trace ~msg ~@struct-args))

(defmacro trace-m
  "Uses `loc-m` on trace level."
  [msg & args]
  `(log-m ~'trace ~msg ~@args))

(defmacro trace-e
  "Uses `log-e` on trace level."
  ([e]
   `(log-e ~'trace ~e))
  ([e ctx]
   `(log-e ~'trace ~e ~ctx))
  ([e ctx msg]
   `(log-e ~'trace ~e ~ctx ~msg)))


(defmacro debug-c
  "Uses `log-c` on debug level."
  ([ctx]
   `(log-c ~'debug ~ctx))
  ([ctx msg]
   `(log-c ~'debug ~ctx ~msg))
  ([ctx msg & struct-args]
   `(log-c ~'debug ~ctx ~msg ~@struct-args)))

(defmacro debug-s
  "Uses `log-s` on trace debug."
  [msg & struct-args]
  `(log-s ~'debug ~msg ~@struct-args))

(defmacro debug-m
  "Uses `log-m` on debug level."
  [msg & args]
  `(log-m ~'debug ~msg ~@args))

(defmacro debug-e
  "Uses `log-e` on debug level."
  ([e]
   `(log-e ~'debug ~e))
  ([e ctx]
   `(log-e ~'debug ~e ~ctx))
  ([e ctx msg]
   `(log-e ~'debug ~e ~ctx ~msg)))


(defmacro info-c
  "Uses `log-c` on info level."
  ([ctx]
   `(log-c ~'info ~ctx))
  ([ctx msg]
   `(log-c ~'info ~ctx ~msg))
  ([ctx msg & args]
   `(log-c ~'info ~ctx ~msg ~@args)))

(defmacro info-s
  "Uses `log-s` on trace info."
  [msg & struct-args]
  `(log-s ~'info ~msg ~@struct-args))

(defmacro info-m
  "Uses `log-m` on info level."
  [msg & args]
  `(log-m ~'info ~msg ~@args))

(defmacro info-e
  "Uses `log-e` on info level."
  ([e]
   `(log-e ~'info ~e))
  ([e ctx]
   `(log-e ~'info ~e ~ctx))
  ([e ctx msg]
   `(log-e ~'info ~e ~ctx ~msg)))


(defmacro warn-c
  "Uses `log-c` on warn level."
  ([ctx]
   `(log-c ~'warn ~ctx))
  ([ctx msg]
   `(log-c ~'warn ~ctx ~msg))
  ([ctx msg & args]
   `(log-c ~'warn ~ctx ~msg ~@args)))

(defmacro warn-s
  "Uses `log-s` on trace warn."
  [msg & struct-args]
  `(log-s ~'warn ~msg ~@struct-args))

(defmacro warn-m
  "Uses `log-m` on warn level."
  [msg & args]
  `(log-m ~'warn ~msg ~@args))

(defmacro warn-e
  "Uses `log-e` on warn level."
  ([e]
   `(log-e ~'warn ~e))
  ([e ctx]
   `(log-e ~'warn ~e ~ctx))
  ([e ctx msg]
   `(log-e ~'warn ~e ~ctx ~msg)))


(defmacro error-c
  "Uses `log-c` on error level."
  ([ctx]
   `(log-c ~'error ~ctx))
  ([ctx msg]
   `(log-c ~'error ~ctx ~msg))
  ([ctx msg & args]
   `(log-c ~'error ~ctx ~msg ~@args)))

(defmacro error-s
  "Uses `log-s` on trace error."
  [msg & struct-args]
  `(log-s ~'error ~msg ~@struct-args))

(defmacro error-m
  "Uses `log-m` on error level."
  [msg & args]
  `(log-m ~'error ~msg ~@args))

(defmacro error-e
  "Uses `log-e` on error level."
  ([e]
   `(log-e ~'error ~e))
  ([e ctx]
   `(log-e ~'error ~e ~ctx))
  ([e ctx msg]
   `(log-e ~'error ~e ~ctx ~msg)))

;; # Spy macro

(defmacro spy
  "A spy macro to log inspection of an expression.
  It will log the value of the expression and the expression itself as context via `log-c`
  with \"spy\" as message.
  By default, it uses the debug level; the diadic version allows to specify the level as string,
  keyword or symbol (e.g. `(spy :info (+ 1 2)) for info level."
  ([expr]
   `(spy :debug ~expr))
  ([level expr]
   (let [method (symbol (name level))]
     `(let [val# ~expr]
        (log-c ~method {:expression (str/trim (with-out-str
                                                (pp/with-pprint-dispatch
                                                  pp/code-dispatch
                                                  (pp/pprint '~expr))))
                        :value      val#}
               "spy")
        val#))))

;; # version functions and macros to fetch the version of a project from different sources

(defmacro version-from-ns-as-class
  ([]
   `(version-from-ns-as-class ~*ns*))
  ([ns]
   (let [ns (str ns)]
     `(try
        (some->
          (Class/forName (str ~ns "__init"))
          (.getPackage)
          (.getImplementationVersion))
        (catch Exception e#
          (debug-e e# {:ns ~ns} "Could not extract the version from the ns (as class)"))))))

(defn version-from-file
  ([]
   (version-from-file version-file-path))
  ([version-file]
   (try
     (-> version-file
         slurp
         edn/read-string
         :version)
     (catch Exception _
       (debug-s "Could not extract the version from the {}" :version-file version-file)))))

(defmacro version
  ([]
   `(version ~*ns*))
  ([ns]
   `(or (version-from-ns-as-class ~ns)
        (version-from-file)
        "undefined")))
