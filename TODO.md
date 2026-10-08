# TODO

## Fix now


## Fix later

- [ ] Fix the 9 medium SpotBugs findings (`mvn -B compile spotbugs:check`): a possible null dereference on an exception path in `IDLabFunctions.create`, `IDLabTestFunctions.random()` hiding the static `IDLabFunctions.random()`, and constructor-throw and exposed-internal-list findings in `SearchParameters`.
- [ ] Add an FnO description for `multipleLookup`.
- [ ] Decide on the `dbpediaSpotlight` test, whose body is commented out because it needs an external endpoint (mock it, tag it as a network test, or remove it).
- [ ] Decide whether `fno_idlab_old/`, with the old namespace, is still needed.

## To triage
