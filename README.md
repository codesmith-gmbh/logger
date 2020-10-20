# Codesmith Logger

[![Clojars Project](https://img.shields.io/clojars/v/codesmith/logger.svg)](https://clojars.org/codesmith/logger)

This library is in ALPHA release; expect API breaking changes.

The `codesmith/logger` library is thin macro layer on top of [Slf4j](http://www.slf4j.org).
It simplifies the usage of the [Logstash Logback Encoder](https://github.com/logstash/logstash-logback-encoder)
in Clojure projects. The Logstash Logback Encoder is an encoder for [Logback](http://logback.qos.ch)
that emits a Logstash compatible JSON string for every logging events. We use the special
[Markers](https://github.com/logstash/logstash-logback-encoder/blob/master/src/main/java/net/logstash/logback/marker/Markers.java)
to log contexts as well [StructuredArguments](https://github.com/logstash/logstash-logback-encoder/blob/master/src/main/java/net/logstash/logback/argument/StructuredArguments.java)
to structure the log entries.

## Usage

### In Clojure

The logging macros are in the namespace `codesmith.logger`.

The library requires a [Logger](https://www.javadoc.io/doc/org.slf4j/slf4j-api/1.7.29/org/slf4j/Logger.html) instance
in every namespace where the logging macros is used.
This is accomplished by calling the macro `deflogger`, typically called at the top of the namespace file. The macro 
creates a var in the namespace with the name `⠇⠕⠶⠻`. You must avoid creating a var with the same name.

```clojure
(ns example
  (require [codesmith.logger :as log]))

(log/deflogger)

;; The logging macros can now be used in the example namespace.
``` 

The logger macros come in three families: context/structured arguments (ending with `-c`), message (ending with `-m`)
and error (ending with `-e`). Each family has a macro for one of the 5 log levels, with prefixes `trace`,
`debug`, `info`, `warn` and `error`. We illustrate for the log level `info`; the others are similar.

#### Macro `info-c`

The macro `info-c` is used to transmit a context and structured argument to the log entry.
With the `LogstashEncoder` (cf. below), the context is added under the key `"context"` in the emitted JSON.
The macro comes with 3 variants: context only; context and message; context, format string and varars with key/value pairs
that are passed as structured arguments.

```clojure
(log/info-c {:key "value"})
;; {"@timestamp":"...","@version":"1","message":"","logger_name":"example",...,"context":{"key":"value"}}

(log/info-c {:key "value"} "important message")
;; {"@timestamp":"...","@version":"1","message":"important message","logger_name":"example",...,"context":{"key":"value"}}

(log/info-c {:key "value"} "import message for {}" :user "stan")
;; {"@timestamp":"...","@version":"1","message":"important message for user=\"stan\"","logger_name":"example",...,"context":{"key":"value"}, "user":"stan"}
```

The `info-c` macro produces code that checks if the info log level is enabled and it guaranties that
the json transformation happens exactly once. The json transformation is handled by the 
library `cheshire`. If the context or structured argument value is not encodable by cheshire,
the context is encoded via `pr-str` and emitted as string. The library logs a warning in that case.

```clojure
(log/info-c {:key +})
;; {"@timestamp":"...","@version":"1","message":"Serialization error","logger_name":"codesmith.logger.core","level":"WARN",...,"stack_trace":"com.fasterxml.jackson.core.JsonGenerationException: Cannot JSON encode object of class: class clojure.core$_PLUS_: clojure.core$_PLUS_@7a5a9ca1...."}
;; {"@timestamp":"...","@version":"1","message":"","logger_name":"example","context":"{:key #object[clojure.core$_PLUS_ 0x7a5a9ca1 \"clojure.core$_PLUS_@7a5a9ca1\"]}"}
``` 

#### Macro `info-m`

The macro `info-m` is used for simple message formating. To have structured arguments, you must use the `log/kv` function.

```clojure
(log/info-m "import message for {}, status {}" (log/kv :user "stan") 400)
;; {"@timestamp":"...","@version":"1","message":"import message for user=stan, status 400","logger_name":"example","user":"stan"...}
```

#### Macro `info-e`

The macro `info-e` is used to log errors (Throwables). The macro comes with 3 variants: error only;
error and message; error, context and message. In the first case, the message from the error
is used as message via `Throwable#getMessage()` and the result of `ex-data` is put ins the json under
the key `exdata` (by default). The dyadic version takes a context as second argument.
The triadic version allows to give an alternative error message.

```clojure
(log/info-e (IllegalStateException. "fatal error"))
;; {"@timestamp":"...","@version":"1","message":"fatal error","logger_name":"example",...,"stack_trace":"..."}

(log/info-e (ex-info "fatal error" {:key "value"}))
;; {"@timestamp":"...","@version":"1","message":"fatal-error","logger_name":"example",...,"stack_trace":"...","exdata":{"key":"value"}}

(log/info-e (ex-info "fatal error" {:key "value"}) {:more "information"})
;; {"@timestamp":"...","@version":"1","message":"fatal error","logger_name":"example",...,"stack_trace":"...","exdata":{"key":"value"},"context":{"more":"information"}}

(log/info-e (ex-info "fatal error" {:key "value"})  {:more "information"} "important message")
;; {"@timestamp":"...","@version":"1","message":"important message","logger_name":"example",...,"stack_trace":"...","exdata":{"key":"value"},"context":{"more":"information"}}
```

As for the `info-c` macro, the `info-e` macro produces code that checks if the info log level is enabled
and it guaranties that the json transformation happens exactly once.
The json transformation is handled by the library `cheshire`.
If the first argument is not a Throwable, the library will wrap a string representation of
that value with `ex-info`. The library logs a warning in that case.

```clojure
(log/info-e 1)
;; {"@timestamp":"???","@version":"1","message":"Value 1 is not a throwable; wrapping in ex-info","logger_name":"codesmith.logger.core","level":"WARN",...}
;; {"@timestamp":"???","@version":"1","message":"1","logger_name":"example",...,"stack_trace":"clojure.lang.ExceptionInfo: 1","exdata":{}}

(log/info-e (ex-info "important message" {:key +}))
;; {"@timestamp":"???","@version":"1","message":"Serialization error","logger_name":"codesmith.logger.core","level":"WARN",...,"stack_trace":"com.fasterxml.jackson.core.JsonGenerationException: Cannot JSON encode object of class: class clojure.core$_PLUS_: clojure.core$_PLUS_@7a5a9ca1..."}
;; {"@timestamp":"???","@version":"1","message":"important message","logger_name":"example",...,"stack_trace":"...","exdata":"{:key #object[clojure.core$_PLUS_ 0x7a5a9ca1 \"clojure.core$_PLUS_@7a5a9ca1\"]}"}
```

#### Macro `spy`

The library has also a `spy` macro to intercept and log an expression and its value. The monadic variant
logs with the `debug` level. The dyadic variant takes the log level as first argument; the log level can
be given as string, keyword or symbol.

```clojure
(log/spy :info (+ 1 2))
;; {"@timestamp":"...","@version":"1","message":"spy","logger_name":"example","context":{"expression":"(+ 1 2)","value":3}}
;; 3
```

### Configuration

#### In Clojure

The library has three functions to configure it: `set-context-logging-key!`, `set-ex-data-logging-key!` and
`set-context-pre-logging-transformation!`. The functions `set-context-logging-key!` and `set-ex-data-logging-key!` take
a string as argument. They configures the string that is used as a key to output the context and the exception data
respectively in the JSON output of the Logstash appender. By default, they are `"context"` and `"exdata"` respectively.

The function `set-context-pre-logging-transformation!` takes a monadic function as an argument. It
is applied to the context before it is encoded in JSON. By default, it is the identity function.
For instance, this function can be used to filter out keys from the context.

#### Logback configuration

For server/productive usage, you want to configure logback appenders with the `LogstashEncoder` encoder.
This will cause logback to emit one line of JSON for every log entry. In the following example, 
we configure the `ConsoleAppender` to use the `LogstashEncoder`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="false" debug="true">
	<statusListener class="ch.qos.logback.core.status.OnConsoleStatusListener" />

	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<encoder class="net.logstash.logback.encoder.LogstashEncoder" />
	</appender>

	<!-- libraries -->
	<logger name="ch.qos.logback.classic" level="WARN"/>
	<logger name="org.apache.http" level="WARN"/>
	<logger name="codesmith.logger" level="WARN"/>

	<root level="INFO">
		<appender-ref ref="STDOUT"/>
	</root>
</configuration>
```

While it is possible to use the previous configuration for development, it can be problematic
to read stack traces of `Throwable` logged events as they will on a (very long) line. For development,
we recommand to use the standard pattern encoder. Use the `%marker` pattern to print out the context.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" debug="true" scanPeriod="20 seconds">
	<statusListener class="ch.qos.logback.core.status.OnConsoleStatusListener" />

	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern>%d{yyyy-MM-dd'T'HH:mm:ss.SSS} %-5level %-18thread - %marker - %msg%n
			</pattern>
		</encoder>
	</appender>

	<!-- libraries -->
	<logger name="ch.qos.logback.classic" level="WARN"/>
	<logger name="org.apache.http" level="INFO"/>
	<logger name="codesmith.logger" level="WARN"/>

	<root level="INFO">
		<appender-ref ref="STDOUT"/>
	</root>
</configuration>
```

You can find these example configurations in the folder `examples`.

## License

Copyright 2017-2018 — AVA-X GmbH (Original code
included in commit [5ab43e44008d08301a16d47dcf6bd8080cc8c288](https://github.com/codesmith-gmbh/logger/commit/5ab43e44008d08301a16d47dcf6bd8080cc8c288))

Copyright 2020 — Codesmith GmbH (All modifications since 5ab43e44008d08301a16d47dcf6bd8080cc8c288)

Codesmith Logger is licensed under Eclipe Public License v1.0
