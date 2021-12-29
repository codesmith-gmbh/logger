# 0.5.53 (2021-12-29 / 6dea47f)
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## 0.3.2

### Fixed
- namespace in documentation and examples.

## 0.3.1

### Removed
- Unused dependency on `org.clojure/tools.deps.alpha`

## 0.3.0

### Added
- BREAKING CHANGE: Support for StructuredArguments in the `log-c` macro.

### Changed
- BREAKING CHANGE: main namespace is now `codesmith.logger`
- BREAKING CHANGE: the dyadic version of the `log-e` macros takes now a context as second argument (to encourage
  passing a context).
- The `log-e` macros emits the `ex-data` under another key (`exdata`) instead of merging the context with the context.
- `nil` can be used as context (nothing is emitted to the JSON).
- arbitrary JSON encodable values can be used as context.
- `antq/antq` version bumped to 0.7.5
- `seancorfield/depstar` version bumped to 1.1.132
- `slipset/deps-deploy` version bumped to 0.1.1

## 0.2.1

### Fixed
- Removed faulty slf4j nop dependency in pom file.

## 0.2.0

### Added
- Logging macros.