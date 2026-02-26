# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Changed
- ....

## [0.6.6] - 2025-10-07
### Changed
- Remove template file enumeration on startup of the Stencil service. Fixes issue https://github.com/erdos/stencil/issues/176

## [0.6.5] - 2025-09-15
### Changed
- Introducing `stencil.fs/unix-path` function
- Additional fixes for issue #175

## [0.6.4] - 2025-09-10
### Changed
- Fix keys starting with digit, issue #175 - a regression introduced in #91

## [0.6.3] - 2025-08-02
### Changed
- Bump base image versions of service Dockerfile to latest stable Debian.

## [0.6.2] - 2025-04-21
### Changed
- Allow substitution of tabulator characters in text runs. #171
- Various small refactors around namespace organization.
- Fix broken regression tests.

## [0.6.1] - 2024-08-03
### Changed
- Update GraalVM version for service
- Fix reflection errors in Docker service
- Introduce functions provider SPI #166
- Add Makefile

## [0.6.0] - 2024-07-26
### Changed
- Refactor stencil.merger` namespace in trampoline style #152

## [0.5.9] - 2024-07-14
### Changed
- Make `replaceImage()` work with `imagedata` tags.
- Introduce `deps.edn` for building and kaocha for tests #164
- Fix: all test namespaces have `-test` suffix

## [0.5.8] - 2024-05-21
### Changed
- Simplified Java API
- Fixed some type hints
- Some internal code reorganization
- Extract fragment handling into a separate namespace #157
- Introduce `stencil.fs` namespace with test cases
- Several fixes in CI: unit test reporting, javadoc generation, etc.
- Fix PPTX file rendering #161

## [0.5.7] - 2023-11-15
### Changed
- Allow expression in fragment include directives #154

## [0.5.6] - 2023-11-03
### Changed
- Fix: backport to work with Java 8

## [0.5.5] - 2023-11-03
### Changed
- Allow overwrite of temporary directory location with `stencil.tmpdir` environment variable.

## [0.5.4] - 2023-10-11
### Changed
- Introduce `replaceLink` function #150

## [0.5.3] - 2023-07-22
### Changed
- improvements to `stencil.util/whitespace?` function

## [0.5.2] - 2023-07-13
### Changed
- Fix: numbering definitions are copied from fragments #148
- Introducing visual regression test
- Refactoring parser code #144 and infix tokenizer #142


## [0.5.1] - 2023-07-12
### Changed
- Introducing visual regression test
- Refactoring parser code #144 and infix tokenizer #142

## [0.5.0] - 2022-12-02
### Changed
- various small refactors to improve code style and remove unused code
- added extract-wordml script to help debugging issues
- fix parsing issue with expressions in form of `x[y] - z`
- fix issue with nonbreaking spaces not being trimmed [#141]
- first steps for more meaningful error messages [#140]

## [0.4.7] - 2022-08-08
### Changes
- Fix: make it possible to call custom functions from fragments
- Fix several linter warnings in the Clojure code base
- Introduce Specs for internal data model #83

## [0.4.6] - 2022-07-25
### Changes
- Fix bug in merger substring expressions #135
- Fix bug in fragment handling #136

## [0.4.5] - 2022-07-21
### Features
- Add new `replace()` function #134
### Changed
- Add unit tests for `coalesce` function
- Bump `junit` version to `4.13.2`

## [0.4.4] - 2022-06-09
### Changed
- Fix precision of `decimal()` and `format()` functions to 8 digits

## [0.4.3] - 2022-06-01
### Features
- Introduce the `foreach` loop construct #129
- Introduce the `pageBreak` function #126

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

## [0.3.31] - 2022-01-16
No changes.

## [0.3.30] - 2022-01-16
### Changes
- fix logs in stencil service
- Add `data()` function to access whole data map, as proposed in https://github.com/erdos/stencil/issues/102
- Improve code test coverage, adding unit tests, removing dead code.

