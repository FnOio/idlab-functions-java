# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2023-07-06

### Added
- Java property `ifState` to point to directory keeping state (at startup time), used when the corresponding function
  argument is `null`. Example usage: `java -DifState=/my/temp/dir ...`

### Changed
- IDLabFunctions: improve implicitDelete documentation

### Fixed
- When a custom state path was given, the state file ended up in the wrong place.

## [0.3.2] - 2023-06-12

### Fixed
- IDLabFunctions: rename 'template' to 'iri'


### Changed
- IDLabFunctions: move state dir path resolving to separate method

### Added
- Map: add hasKey method
- Map: add getEntries method
- Map: add replace method
- Map: add remove method
- IDLabFunctions: add implicitCreate function
- IDLabFunctions: add implicitUpdate function
- IDLabFunctions: add implicitDelete function

## [0.3.1] - 2023-05-04

### Fixed
- Generate truly unique IRIs, even if the template is the same (fixes internal GitLab [issue #9](https://gitlab.ilabt.imec.be/KNoWS/fno/lib/idlab-functions-java/-/issues/9) )

### Added
- A method `MapState.putAndReturnIndex()` which returns the number of associated values with a given key.

## [0.3.0] - 2023-04-24

### Changed
- `IDLabFunctions.generateUniqueIRI` now also takes `null` as values for `isUnique` and 
`stateDirPathStr`, and also special values `__tmp` and `__working_dir` for `stateDirPathStr`. 

## [0.2.0] - 2023-01-12

### Changed
- Added close() method to IDLabFunctions.
- Added a `ShutDownHook` to close everything properly when JVM stops for some reason.
- GenerateUniqueIRI: use nr of unique IRIs generated so far to generate next unique IRI.

### Fixed
- `IDLabFunctions.generateUniqueIRI`: sort watched properties before checking, because the order in the given input string may vary.

## [0.1.5] - 2022-12-12
- MapDBContainer: creates parent dir of state file if it doesn't exist. (IDLab issue [#5](https://gitlab.ilabt.imec.be/KNoWS/fno/lib/idlab-functions-java/-/issues/5))

## [0.1.4] - 2022-12-09

### Changed
- Replaced SimpleInMemoryMapState by MapDBState; this uses memory mapped files to keep state.

## [0.1.3] - 2022-12-08

### Changed
- Requires Java 11+ 
- Updated JUnit from 4.11 to 5.9.1
- Updated Jackson core from 2.13.1 to 2.14.1
- Updated OpenCSV from 5.5.2 to 5.7.1
- Updated Slugify from 2.5 to 3.0.2
- Updated SLF4J from 1.7.36 to 2.0.5

### Fixed
- Refactored generation of unique IRIs (fixes internal GitLab issue [#4](https://gitlab.ilabt.imec.be/KNoWS/fno/lib/idlab-functions-java/-/issues/4)).

## [0.1.2] - 2022-09-02

### Added
- concatSequence function

## [0.1.1] - 2022-09-01

### Added
- Multi-lookup function

## [0.1.0] - 2022-04-23

### Added

- Separate implementation mapping for IDLab test functions

## [0.0.1] - 2022-04-22

### Added

- Code extracted from [RMLMapper](https://github.com/RMLio/rmlmapper-java)

[1.0.0]: https://github.com/FnOio/idlab-functions-java/compare/v0.3.1...v1.0.0
[0.3.2]: https://github.com/FnOio/idlab-functions-java/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/FnOio/idlab-functions-java/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/FnOio/idlab-functions-java/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/FnOio/idlab-functions-java/compare/v0.1.5...v0.2.0
[0.1.5]: https://github.com/FnOio/idlab-functions-java/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/FnOio/idlab-functions-java/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/FnOio/idlab-functions-java/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/FnOio/idlab-functions-java/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/FnOio/idlab-functions-java/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/FnOio/idlab-functions-java/compare/v0.0.1...v0.1.0
[0.0.1]: https://github.com/FnOio/idlab-functions-java/releases
