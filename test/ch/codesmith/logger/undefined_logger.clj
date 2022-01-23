(ns ch.codesmith.logger.undefined-logger
  (:require [clojure.test :refer [deftest is]]
            [ch.codesmith.logger :as log])
  (:import [clojure.lang Compiler$CompilerException]))

(deftest undefined-logger
  (is (thrown? Compiler$CompilerException (macroexpand `(log/info-m "test")))))
