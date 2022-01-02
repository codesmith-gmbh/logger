(ns codesmith.logger.undefined-logger
  (:require [clojure.test :refer :all]
            [ch.codesmith.logger :as log])
  (:import [clojure.lang Compiler$CompilerException]))

(deftest undefined-logger
  (is (thrown? Compiler$CompilerException (macroexpand `(log/info-m "test")))))
