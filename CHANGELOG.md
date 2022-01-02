# 0.6.61 (2022-01-02 / e2c89bc)

## Added

## Fixed

- Fix #8 : Use qualified namespaces when encoding StructuredArguments

## Changed

- Moved the group + package prefix from codesmith to ch.codesmith

# 0.5.57 (2021-12-30 / 3e8d343)

## Added

## Fixed

- documentation
- building/releasing scripts

## Changed

# 0.5.54 (2021-12-29 / 3756776)

## 0.4.X

## Changes

- jsonista for JSON encoding

# 0.3.2

## Fixed
- namespace in documentation and examples.

# 0.3.1

## Removed
- Unused dependency on `org.clojure/tools.deps.alpha`

# 0.3.0

## Added
- BREAKING CHANGE: Support for StructuredArguments in the `log-c` macro.

## Changed
- BREAKING CHANGE: main namespace is now `ch.codesmith.logger`
- BREAKING CHANGE: the dyadic version of the `log-e` macros takes now a context as second argument (to encourage
  passing a context).
- The `log-e` macros emits the `ex-data` under another key (`exdata`) instead of merging the context with the context.
- `nil` can be used as context (nothing is emitted to the JSON).
- arbitrary JSON encodable values can be used as context.
- `antq/antq` version bumped to 0.7.5
- `seancorfield/depstar` version bumped to 1.1.132
- `slipset/deps-deploy` version bumped to 0.1.1

# 0.2.1

## Fixed
- Removed faulty slf4j nop dependency in pom file.

# 0.2.0

## Added
- Logging macros.