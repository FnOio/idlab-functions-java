package be.ugent.knows.idlabFunctions.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SimpleInMemorySingleValueMapState<K, V> implements MapState<V, K, V> {

    private final static Logger log = LoggerFactory.getLogger(SimpleInMemoryMapState.class);
    private final Map<String, Map<K, V>> stateFileToMap = new HashMap<>();
    @Override
    public synchronized V put(String stateFilePath, K key, V value) {
        Map<K, V> map = computeMap(stateFilePath);
        return map.put(key, value);
    }

    @Override
    public synchronized Optional<Integer> putAndReturnIndex(String stateFilePath, K key, V value) {
        Map<K, V> map = computeMap(stateFilePath);
        if (map.containsKey(key)) {
            V knownValue = map.get(key);
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
    public synchronized Optional<Integer> replaceAndReturnIndex(String stateFilePath, K key, V value) {
        // for a single value, this is the same as putAndReturnIndex
        return putAndReturnIndex(stateFilePath, key, value);
    }

    @Override
    public void replace(String stateFilePath, K key, V value) {
        put(stateFilePath, key, value);
    }

    @Override
    public synchronized boolean hasKey(String stateFilePath, K key) {
        Map<K, V> map = computeMap(stateFilePath);
        return map.containsKey(key);
    }

    @Override
    public Map<K, V> getEntries(String stateFilePath) {
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
    public long count(String stateFilePath, K key) {
        Map<K, V> map = computeMap(stateFilePath);
        return map.containsKey(key) ? 1 : 0;
    }

    @Override
    public synchronized void remove(String stateFilePath, K key) {
        Map<K, V> map = computeMap(stateFilePath);
        map.remove(key);
    }

    @Override
    public synchronized void close() throws Exception {
        saveAllState();
        stateFileToMap.clear();
    }

    private synchronized Map<K, V> computeMap(final String stateFilePath) {
        return stateFileToMap.computeIfAbsent(stateFilePath, mapKey -> {
            // first check if file exists and try to load map
            File stateFile = new File(stateFilePath);
            Map<K, V> newMap = new HashMap<>();
            if (stateFile.exists() && stateFile.isFile() && stateFile.canRead()) {
                try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(stateFilePath)))){
                    newMap = (Map<K, V>)in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    log.warn("Cannot load state map from file {}. Creating empty map!", stateFilePath);
                }
            }
            return newMap;
        });
    }
}
