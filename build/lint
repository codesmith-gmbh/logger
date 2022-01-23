#!/usr/bin/env bash

set -eo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

if command -v clj-kondo &>/dev/null; then
  CONDO_CMD="$(command -v clj-kondo)"
else
  CONDO_CMD="clojure -M:clj-kondo"
fi

(
  cd "${SCRIPT_DIR}"/.. || exit 1
  $CONDO_CMD --lint "$(clojure -A:test -Spath)" --parallel --dependencies --copy-configs
  $CONDO_CMD --lint "src:test"
)
