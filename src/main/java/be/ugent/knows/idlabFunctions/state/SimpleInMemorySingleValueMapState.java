package be.ugent.knows.idlabFunctions.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SimpleInMemorySingleValueMapState implements MapState<String> {

    private final static Logger log = LoggerFactory.getLogger(SimpleInMemoryMapState.class);
    private final Map<String, Map<String, String>> stateFileToMap = new HashMap<>();
    @Override
    public synchronized String put(String stateFilePath, String key, String value) {
        Map<String, String> map = computeMap(stateFilePath);
        return map.put(key, value);
    }

    @Override
    public synchronized Optional<Integer> putAndReturnIndex(String stateFilePath, String key, String value) {
        Map<String, String> map = computeMap(stateFilePath);
        if (map.containsKey(key)) {
            String knownValue = map.get(key);
            if (knownValue.equals(value)) {
                return Optional.empty();
            } else {
                map.put(key, value);
                return Optional.of(0);
            }
        } else {
            map.put(key, value);
            return Optional.of(0);
        }
    }

    @Override
    public synchronized Optional<Integer> replaceAndReturnIndex(String stateFilePath, String key, String value) {
        // for a single value, this is the same as putAndReturnIndex
        return putAndReturnIndex(stateFilePath, key, value);
    }

    @Override
    public void replace(String stateFilePath, String key, String value) {
        put(stateFilePath, key, value);
    }

    @Override
    public synchronized boolean hasKey(String stateFilePath, String key) {
        Map<String, String> map = computeMap(stateFilePath);
        return map.containsKey(key);
    }

    @Override
    public Map<String, String> getEntries(String stateFilePath) {
        return this.computeMap(stateFilePath);
    }

    @Override
    public synchronized void deleteAllState() {
        stateFileToMap.forEach((stateFilePath, stateMap) -> {
            File stateFile = new File(stateFilePath);
            if (!stateFile.delete()) {
                log.warn("Could not delete {}", stateFilePath);
            }
        });
        stateFileToMap.clear();
    }

    @Override
    public synchronized void saveAllState() {
        stateFileToMap.forEach((stateFilePath, stateMap) -> {
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(stateFilePath, false)))) {
                out.writeObject(stateMap);
            } catch (IOException e) {
                log.warn("Cannot save state map to {}", stateFilePath);
            }
        });
    }

    @Override
    public long count(String stateFilePath, String key) {
        Map<String, String> map = computeMap(stateFilePath);
        return map.containsKey(key) ? 1 : 0;
    }

    @Override
    public synchronized void remove(String stateFilePath, String key) {
        Map<String, String> map = computeMap(stateFilePath);
        map.remove(key);
    }

    @Override
    public synchronized void close() throws Exception {
        saveAllState();
        stateFileToMap.clear();
    }

    private synchronized Map<String, String> computeMap(final String stateFilePath) {
        return stateFileToMap.computeIfAbsent(stateFilePath, mapKey -> {
            // first check if file exists and try to load map
            File stateFile = new File(stateFilePath);
            Map<String, String> newMap = new HashMap<>();
            if (stateFile.exists() && stateFile.isFile() && stateFile.canRead()) {
                try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(stateFilePath)))){
                    newMap = (Map<String, String>)in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    log.warn("Cannot load state map from file {}. Creating empty map!", stateFilePath);
                }
            }
            return newMap;
        });
    }
}
