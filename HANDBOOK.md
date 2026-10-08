# idlab-functions-java handbook

Written for a CS student who wants to understand idlab-functions-java as code.

## Contents

- [Preface](#preface)
- [Agent request contract (for AI agents/LLMs)](#agent-request-contract-for-ai-agentsllms)
- [Architecture](#architecture)
- [Function descriptions (FnO)](#function-descriptions-fno)
- [State](#state)
- [File resolution and the lookup cache](#file-resolution-and-the-lookup-cache)
- [Build and test](#build-and-test)
- [Release process](#release-process)

## Preface

idlab-functions-java is a Java 21 library of basic functions used by tools from
[KNoWS - IDLab](https://knows.idlab.ugent.be/), most notably RML mapping engines
that execute functions through the [Function Ontology](https://fno.io/) (FnO).
Every function is a `public static` Java method, and every exposed function is
semantically described in Turtle files shipped inside the JAR. The library is a
plain Maven JAR (`be.ugent.idlab.knows:idlab-functions-java`); it has no CLI and
no `main` entry point.

Where things live:

- Source: `src/main/java/be/ugent/knows/` (packages `idlabFunctions`, `idlabFunctions.state`, `util`).
- FnO descriptions: `src/main/resources/fno/` (current, `https://w3id.org/imec/idlab/function#` namespace) and `src/main/resources/fno_idlab_old/` (the earlier `http://example.com/idlab/function/` namespace).
- Tests: `src/test/java/be/ugent/knows/idlabFunctions/` (JUnit 5), with CSV fixtures in `src/test/resources/`.
- Build manifest: `pom.xml`; CI: `.gitlab-ci.yml`; release script: `bump-version.sh`.
- User documentation: `README.md`; release notes: `CHANGELOG.md` (Keep a Changelog format).

## Agent request contract (for AI agents/LLMs)

<!-- software-handbook contract: 2026-10-08 -->

Every implementation request handled by an AI agent/LLM follows these constraints:

- If the request is a feature or bugfix:
  - fix the specific failing case or issue named in the request;
  - preserve existing passing behavior unless explicitly asked not to;
  - add or update a regression test when needed.
- Make the smallest coherent patch. A documentation error found along the way is fixed in the same patch.
- Leave the code leaner after every request: remove what the change makes redundant (duplicate tests, parameters and options that no longer do anything, helpers that duplicate each other, comments that only repeat the code), and reuse shared functionality instead of adding a local variant. Use SpotBugs (`mvn -B compile spotbugs:check`, see Build and test), compiler warnings and IDE inspections to find unused code.
- Fix a transient environment problem (a stale PATH, a shell or editor that needs a restart) in the environment, by restarting or reconfiguring it; add no code that works around it.
- **Push back** when a request would violate an established principle (e.g. breaking test hermeticity). Explain the principle and suggest a documentation-only fix instead of silently implementing the harmful change.
- Update this handbook so the change is documented as well as implemented.
  - Document only the latest state, integrated in the surrounding narrative (principles, behavior, rationale), including the choices made and why.
  - This contract holds only general rules for handling a request; project-specific guidance goes in the chapter on that topic.
- Do not stop at making tests green; align the implementation with the specification or intended design, and document the semantic reason in this handbook.
- Never remove or change existing tests (code or fixtures) without explicit permission. A change to an existing fixture (expected output, input, or data) is validated by the maintainer before it is kept, also when a tool writes it: propose the change with its reason, and keep it only after approval.
- Update `CHANGELOG.md` for implementation changes: keep `## Unreleased` a short summary of what changed since the last release. A feature that is new since the last release is one Added line, which later fixes update instead of getting lines of their own; lines are for what a user of the last release notices.
- Check whether `README.md` needs updates for user-visible behavior or workflow changes, and update it when needed.
- Write documentation (this handbook, READMEs, `TODO.md`, `CHANGELOG.md`, code comments) as plain positive statements: say what is true and leave out the contrast ("X, not Y"). Keep a negative only when it is the point itself, such as a prohibition, a warning, or a known limitation.
- If there are difficulties during fulfillment, document them in the most appropriate existing handbook location (create a new chapter only when truly necessary) so future requests start with better context.
- A preference or principle that the maintainer states while handling a request is documented so that every later request follows it: a general one in this contract (and in the software-handbook skill it comes from), a project-specific one in the handbook chapter it belongs to. When it is unclear which, ask.
- When a request is a list of feedback (such as a `TODO.md`), clean up after handling it: remove the items that are done, keep every open item as a clear task (an open question or an offered follow-up is an open item), and remove temporary files created along the way.

## Architecture

Package `be.ugent.knows.idlabFunctions`:

- `IDLabFunctions` holds nearly all functions: string and list predicates, `decide`/`trueCondition`/`isNull`, MIME type and file reading, `slugify`, date normalization, `concat`/`concatSequence`/`crossConcatSequence`, `jsonize`, CSV `lookup`/`lookupWithDelimiter`/`multipleLookup`, `dbpediaSpotlight`, the stateful LDES change-detection functions (`generateUniqueIRI`, `createUniqueIRI`, `updateUniqueIRI`, `implicit*`/`explicit*` create/update/delete), and `alwaysReturnsABC` (used by the RML-FNML test cases).
- `UtilFunctions` holds `equal` and `notEqual`.
- `IDLabTestFunctions` holds test helpers exposed through FnO (`random`, `getNull`, `generateA`).
- `MIMETypes` is the static extension-to-MIME-type map behind `getMIMEType`.

Package `be.ugent.knows.idlabFunctions.state` defines the `SetState` and `MapState` interfaces and their in-memory implementations (`SimpleInMemorySetState`, `SimpleInMemoryMapState`, `SimpleInMemorySingleValueMapState`), which keep state in memory and persist it to a file.

Package `be.ugent.knows.util` holds `Utils` (file resolution, file reading, directory deletion), `Cache` (the CSV row cache for lookups) and `SearchParameters`.

## Function descriptions (FnO)

`src/main/resources/fno/functions_idlab.ttl` describes each function (`fno:Function`, parameters, outputs) and maps it to a Java method name through `fno:methodMapping` / `fnom:StringMethodMapping`. `functions_idlab_classes_java_mapping.ttl` maps the implementation IRIs to the classes `IDLabFunctions` and `UtilFunctions`; `functions_idlab_test_classes_java_mapping.ttl` additionally maps `IDLabTestFunctions`. The `fno_idlab_old/` directory keeps the same structure under the earlier namespace for engines that still use it.

A function becomes usable from an FnO-aware engine only once it is described in these Turtle files; a Java method alone is invisible to such engines. `multipleLookup` currently exists in Java only and has no FnO description.

## State

The LDES change-detection functions remember what they have seen across calls and runs. `IDLabFunctions` keeps one static state object per function family; `saveState()` persists all of them, `resetState()` deletes them, and `close()` persists and closes them and clears the state-path cache.

Each stateful function takes a `stateDirPathStr` argument, resolved by `resolveStateDirPath`:

- `__tmp`: the system temporary directory;
- `__working_dir`: the current working directory;
- any other value: that directory, created when missing (falling back to the temporary directory when creation fails);
- `null` or empty: the value of the `ifState` system property (`java -DifState=/path ...`), defaulting to `__tmp`.

Resolved paths are cached per directory/state-file pair. The README documents this for users.

## File resolution and the lookup cache

`Utils.getFile(path)` resolves a path in this order: an absolute path as is; relative to `user.dir`; relative to the parent of `user.dir`; as a classpath resource. All file-reading functions, including the lookups, use this resolution.

`lookupWithDelimiter` and `multipleLookup` read the whole CSV file once through `Cache.getRows`, keyed on the canonical file path and the delimiter, and share that cache. Keying on the canonical path keeps results from different files apart.

## Build and test

- Build the JAR (plus a `jar-with-dependencies` from the assembly plugin): `mvn package`.
- Install locally: `mvn install`.
- Run tests: `mvn test`. CI runs `mvn $MAVEN_CLI_OPTS test` on JDK 21 (`maven:3-eclipse-temurin-21-alpine`) for every branch except `main`.
- Lint with SpotBugs: `mvn -B compile spotbugs:check`. The plugin (`spotbugs-maven-plugin` 4.10.3.0 with SpotBugs 4.10.3, the setup MappingWeaver-java uses) is version-locked in `pluginManagement` and bound to no lifecycle phase, so `mvn package` and CI never fail on findings; the check runs on demand. The known state is 9 findings (all medium): a possible null dereference on an exception path in `IDLabFunctions.create`, `IDLabTestFunctions.random()` hiding the static `IDLabFunctions.random()`, and constructor-throw and exposed-internal-list findings in `SearchParameters`. They are recorded here and not yet fixed.
- CI also includes shared templates from `rml/util/ci-templates`: a check that `CHANGELOG.md` is updated, and the Maven Central deploy job.

Tests:

- `IDLabFunctionsTest` covers the stateless functions and the lookups. Lookup tests read the CSV fixtures in `src/test/resources/` (`class.csv`, `classB.csv`, `student.csv`, `students.csv`, `studentsCopy.csv`) or write temporary CSV files. The `dbpediaSpotlight` test body is commented out because it needs an external endpoint, so the suite runs without network access.
- `LDESGenerationTests` covers the stateful LDES functions; it deletes the `state_file` in the temporary directory before and after each test and calls `IDLabFunctions.resetState()` after each test so tests stay independent.
- `state/StateTest` covers the state implementations.
- `src/test/resources/simplelogger.properties` sets test logging to `debug`.

## Release process

Releases are scripted by `bump-version.sh <version>`: it sets the version in `pom.xml` (`mvn versions:set`), updates the dependency snippet version in `README.md`, optionally adds the version section to `CHANGELOG.md` with `changefrog`, and optionally commits, tags `v<version>` and pushes the tag. The `release` Maven profile builds source and Javadoc JARs, signs with GPG and publishes to Maven Central through `central-publishing-maven-plugin`; `.m2/settings.xml` reads the Central credentials from `MAVEN_REPO_USER` / `MAVEN_REPO_PASS`. Finally, after a pushed release other than a `testrelease-*`, it moves the version to the next patch `-SNAPSHOT` (e.g. `1.5.2-SNAPSHOT` after `1.5.1`) and commits and pushes that as "Prepare for next development cycle".
