(ns codesmith.logger.core-test
  (:require [clojure.test :refer :all]
            [codesmith.logger.core :as log])
  (:import [org.slf4j Logger]))

(log/deflogger)

(set! *warn-on-reflection* true)

(deftest proper-meta
  (is (= Logger (:tag (meta #'⠇⠕⠶⠻)))))

(deftest trace-logs
  (is (not (log/trace-c {})))
  (is (not (log/trace-c {} "hello")))
  (is (not (log/trace-c {} "hello {}" 1)))
  (is (not (log/trace-c {} "hello {} {}" (+ 1 2) 3)))
  (is (not (log/trace-c {} "hello {} {} {}" (+ 1 2) 3 5)))

  (is (not (log/trace-m "hello")))
  (is (not (log/trace-m "hello {}" 1)))
  (is (not (log/trace-m "hello {} {}" 1 2)))
  (is (not (log/trace-m "hello {} {} {}" 1 2 3)))

  (is (not (log/trace-e (ex-info "hello" {}))))
  (is (not (log/trace-e (ex-info "hello" {}) "hello 1")))
  (is (not (log/trace-e (ex-info "hello" {}) {} "hello 2"))))

(deftest debug-logs
  (is (not (log/debug-c {})))
  (is (not (log/debug-c {} "hello")))
  (is (not (log/debug-c {} "hello {}" 1)))
  (is (not (log/debug-c {} "hello {} {}" 1 3)))
  (is (not (log/debug-c {} "hello {} {} {}" 1 3 5)))

  (is (not (log/debug-m "hello")))
  (is (not (log/debug-m "hello {}" 1)))
  (is (not (log/debug-m "hello {} {}" 1 2)))
  (is (not (log/debug-m "hello {} {} {}" 1 2 3)))

  (is (not (log/debug-e (ex-info "hello" {}))))
  (is (not (log/debug-e (ex-info "hello" {}) "hello 1")))
  (is (not (log/debug-e (ex-info "hello" {}) {} "hello 2"))))

(deftest info-logs
  (is (not (log/info-c {})))
  (is (not (log/info-c {} "hello")))
  (is (not (log/info-c {} "hello {}" 1)))
  (is (not (log/info-c {} "hello {} {}" 1 3)))
  (is (not (log/info-c {} "hello {} {} {}" 1 3 5)))

  (is (not (log/info-m "hello")))
  (is (not (log/info-m "hello {}" 1)))
  (is (not (log/info-m "hello {} {}" 1 2)))
  (is (not (log/info-m "hello {} {} {}" 1 2 3)))

  (is (not (log/info-e (ex-info "hello" {}))))
  (is (not (log/info-e (ex-info "hello" {}) "hello 1")))
  (is (not (log/info-e (ex-info "hello" {}) {} "hello 2"))))

(deftest warn-logs
  (is (not (log/warn-c {})))
  (is (not (log/warn-c {} "hello")))
  (is (not (log/warn-c {} "hello {}" 1)))
  (is (not (log/warn-c {} "hello {} {}" 1 3)))
  (is (not (log/warn-c {} "hello {} {} {}" 1 3 5)))

  (is (not (log/warn-m "hello")))
  (is (not (log/warn-m "hello {}" 1)))
  (is (not (log/warn-m "hello {} {}" 1 2)))
  (is (not (log/warn-m "hello {} {} {}" 1 2 3)))

  (is (not (log/warn-e (ex-info "hello" {}))))
  (is (not (log/warn-e (ex-info "hello" {}) "hello 1")))
  (is (not (log/warn-e (ex-info "hello" {}) {} "hello 2"))))

(deftest error-logs
  (is (not (log/error-c {})))
  (is (not (log/error-c {} "hello")))
  (is (not (log/error-c {} "hello {}" 1)))
  (is (not (log/error-c {} "hello {} {}" 1 3)))
  (is (not (log/error-c {} "hello {} {} {}" 1 3 5)))

  (is (not (log/error-m "hello")))
  (is (not (log/error-m "hello {}" 1)))
  (is (not (log/error-m "hello {} {}" 1 2)))
  (is (not (log/error-m "hello {} {} {}" 1 2 3)))

  (is (not (log/error-e (ex-info "hello" {}))))
  (is (not (log/error-e (ex-info "hello" {}) "hello 1 ")))
  (is (not (log/error-e (ex-info "hello" {}) {} "hello 2"))))
