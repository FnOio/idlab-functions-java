# idlab-functions-java

Basic functions commonly used by tools developed at [KNoWS - IDLab](https://knows.idlab.ugent.be/).

The functions in this library are [semantically described](src/main/resources/fno)
using the [Function Ontology](https://fno.io/).

## Building

To build a JAR with dependecies:

```
mvn package
```

To install in your local maven repo:

```
mvn install
```

## State
Some functions keep state persisted on disk. 
Each such function has an argument `stateDirPathStr` where one can specify the location of the state.

Instead of using the argument `stateDirPathStr`, one can use the property `ifState` and pass `null` to `stateDirPathStr`.
The java process then needs to be started as `java -DifState=/path/to/your/state/directory ...`