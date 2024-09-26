# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Changed
- ....

## [0.5.0] - 2022-12-02
### Changed
- various small refactors to improve code style and remove unused code
- added extract-wordml script to help debugging issues
- fix parsing issue with expressions in form of `x[y] - z`
- fix issue with nonbreaking spaces not being trimmed [#141]
- first steps for more meaningful error messages [#140]

## [0.4.2] - 2022-05-04
### Changed
- service: fix division error on non-terminating decimal expansion due to BigDecimal usage in service.

## [0.4.1] - 2022-04-21
### Changed
- service: update dockerfile to use debian:10-slim
- service: update clojure, slf4j versions
- chore: some simplifications

## [0.4.0] - 2022-02-15
### Changed
- service: add js engine [#114]
- core: introduce more transducers
- core: `html()` function supports uppercase tags [#113]
- core: newlines are kept in str substitution [#111]
