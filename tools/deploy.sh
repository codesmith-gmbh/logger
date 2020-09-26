#!/usr/bin/env bash

set -e

rm -fr target
mkdir target
clojure -M:test:runner
clojure -M:jar
clojure -M:deploy
