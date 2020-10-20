(ns codesmith.logger.undefined-logger
  (:require [clojure.test :refer :all]
            [codesmith.logger :as log])
  (:import [clojure.lang Compiler$CompilerException]))

(deftest undefined-logger
  (is (thrown? Compiler$CompilerException (macroexpand `(log/info-m "test")))))
