(ns ch.codesmith.logger.logger-test
  (:require [clojure.test :refer [deftest is]]
            [ch.codesmith.logger :as log])
  (:import [org.slf4j Logger]))

(log/deflogger)

(set! *warn-on-reflection* true)

(deftest fullname-test-correctness
  (is (= "a" (log/fullname "a")))
  (is (= "a" (log/fullname :a)))
  (is (= "a" (log/fullname 'a)))

  (is (= "a/b" (log/fullname "a/b")))
  (is (= "a/b" (log/fullname :a/b)))
  (is (= "a/b" (log/fullname 'a/b))))

(deftest proper-meta
  (is (= Logger (:tag (meta #_{:clj-kondo/ignore [:unresolved-symbol]} #'⠇⠕⠶⠻)))))

(deftest level-pred-test
  (is (= 'isTraceEnabled (log/level-pred 'trace)))
  (is (= 'isDebugEnabled (log/level-pred 'debug)))
  (is (= 'isInfoEnabled (log/level-pred 'info)))
  (is (= 'isWarnEnabled (log/level-pred 'warn)))
  (is (= 'isErrorEnabled (log/level-pred 'error))))

(deftest trace-logs
  (is (not (log/trace-c {})))
  (is (not (log/trace-c {} "hello")))
  (is (not (log/trace-c {} "hello {}" :first 1)))
  (is (not (log/trace-c {} "hello {} {}" :first (+ 1 2) :second {:a 1})))
  (is (not (log/trace-c {} "hello {} {} {}" :first (+ 1 2) :second {:a 1} :third 5)))

  (is (not (log/trace-m "hello")))
  (is (not (log/trace-m "hello {}" 1)))
  (is (not (log/trace-m "hello {} {}" 1 2)))
  (is (not (log/trace-m "hello {} {} {}" 1 2 3)))

  (is (not (log/trace-e (ex-info "hello" {}))))
  (is (not (log/trace-e (ex-info "hello" {}) {:a "hello 1"})))
  (is (not (log/trace-e (ex-info "hello" {}) {:a "hello 1"} "hello 2"))))

(deftest debug-logs
  (is (not (log/debug-c {})))
  (is (not (log/debug-c {} "hello")))
  (is (not (log/debug-c {} "hello {}" :first 1)))
  (is (not (log/debug-c {} "hello {} {}" :first 1 :second {:a 1})))
  (is (not (log/debug-c {} "hello {} {} {}" :first 1 :second {:a 1} :third 5)))

  (is (not (log/debug-m "hello")))
  (is (not (log/debug-m "hello {}" 1)))
  (is (not (log/debug-m "hello {} {}" 1 2)))
  (is (not (log/debug-m "hello {} {} {}" 1 2 3)))

  (is (not (log/debug-e (ex-info "hello" {}))))
  (is (not (log/debug-e (ex-info "hello" {}) {:a "hello 1"})))
  (is (not (log/debug-e (ex-info "hello" {}) {:a "hello 1"} "hello 2"))))

(deftest info-logs
  (is (not (log/info-c {})))
  (is (not (log/info-c {} "hello")))
  (is (not (log/info-c {} "hello {}" :first 1)))
  (is (not (log/info-c {} "hello {} {}" :first 1 :second {:a 1})))
  (is (not (log/info-c {} "hello {} {} {}" :first 1 :second {:a 1} :third 5)))

  (is (not (log/info-m "hello")))
  (is (not (log/info-m "hello {}" 1)))
  (is (not (log/info-m "hello {} {}" 1 2)))
  (is (not (log/info-m "hello {} {} {}" 1 2 3)))

  (is (not (log/info-e (ex-info "hello" {}))))
  (is (not (log/info-e (ex-info "hello" {}) {:a "hello 1"})))
  (is (not (log/info-e (ex-info "hello" {}) {:a "hello 1"} "hello 2"))))

(deftest warn-logs
  (is (not (log/warn-c {})))
  (is (not (log/warn-c {} "hello")))
  (is (not (log/warn-c {} "hello {}" :first 1)))
  (is (not (log/warn-c {} "hello {} {}" :first 1 :second {:a 1})))
  (is (not (log/warn-c {} "hello {} {} {}" :first 1 :second {:a 1} :third 5)))

  (is (not (log/warn-m "hello")))
  (is (not (log/warn-m "hello {}" 1)))
  (is (not (log/warn-m "hello {} {}" 1 2)))
  (is (not (log/warn-m "hello {} {} {}" 1 2 3)))

  (is (not (log/warn-e (ex-info "hello" {}))))
  (is (not (log/warn-e (ex-info "hello" {}) {:a "hello 1"})))
  (is (not (log/warn-e (ex-info "hello" {}) {:a "hello 1"} "hello 2"))))

(deftest error-logs
  (is (not (log/error-c {})))
  (is (not (log/error-c {} "hello")))
  (is (not (log/error-c {} "hello {}" :first 1)))
  (is (not (log/error-c {} "hello {} {}" :first 1 :second {:a 1})))
  (is (not (log/error-c {} "hello {} {} {}" :first 1 :second {:a 1} :third 5)))

  (is (not (log/error-m "hello")))
  (is (not (log/error-m "hello {}" 1)))
  (is (not (log/error-m "hello {} {}" 1 2)))
  (is (not (log/error-m "hello {} {} {}" 1 2 3)))

  (is (not (log/error-e (ex-info "hello" {}))))
  (is (not (log/error-e (ex-info "hello" {}) {:a "hello 1"})))
  (is (not (log/error-e (ex-info "hello" {}) {:a "hello 1"} "hello 2"))))

(deftest string-coercing
  (is (not (log/info-m {:a 1}))))

(deftest constant-coercing
  (is (not (log/error-m "hello {}" 1)))
  (is (not (log/error-m "hello {}" 1.2)))
  (is (not (log/error-m "hello {}" true))))

(deftest throwable-coercion
  (is (not (log/error-e "hello"))))

(deftest encode-markers
  (is (not (log/info-c {:username "stan" :special {:a 1}})))
  (is (not (log/info-c {:username "stan" :special {:a +}}))))

(deftest spy-test
  (is (= 1 (log/spy :info (+ 1 0)))))
