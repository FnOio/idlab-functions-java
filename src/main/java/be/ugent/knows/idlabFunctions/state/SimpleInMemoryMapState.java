package be.ugent.knows.idlabFunctions.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public class SimpleInMemoryMapState<K, V> implements MapState<List<V>, K, V> {
    private final static Logger log = LoggerFactory.getLogger(SimpleInMemoryMapState.class);
    private final Map<String, Map<K, List<V>>> stateFileToMap = new HashMap<>();

    private synchronized Map<K, List<V>> computeMap(final String stateFilePath) {

        return stateFileToMap.computeIfAbsent(stateFilePath, mapKey -> {
            // first check if file exists and try to load map
            File stateFile = new File(stateFilePath);
            Map<K, List<V>> newMap = new HashMap<>();
            if (stateFile.exists() && stateFile.isFile() && stateFile.canRead()) {
                try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(stateFilePath)))){
                    newMap = (Map<K, List<V>>)in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    log.warn("Cannot load state map from file {}. Creating empty map!", stateFilePath);
                }
            }
            return newMap;
        });
    }

    /**
     * Loads or creates a new state map for the given state file path.
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <br>
     * Note that the state is only persisted to file upon {@link #close()}.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key},
     *         if the implementation supports {@code null} values.)
     * @param stateFilePath The path of this state map's persistance file.
     */
    @Override
    public synchronized V put(final String stateFilePath, final K key, final V value) {
        Map<K, List<V>> map = this.computeMap(stateFilePath);
        List<V> values = map.computeIfAbsent(key, k -> new ArrayList<>());  // TODO perf: verander in map (ten koste van memory)
        if (values.isEmpty()) {
            values.add(value);
            map.put(key, values);
            return null;
        } else {
            // TODO: return laatste, en voeg niet toe als al in zit. Voorzie ook functie die index van value teruggeeft
            if (values.contains(value)) {
                return values.get(values.size() - 1);
            } else {
                V returnValue = values.get(values.size() - 1);
                values.add(value);
                map.put(key, values);
                return returnValue;
            }
        }
    }

    @Override
    public synchronized Optional<Integer> putAndReturnIndex(String stateFilePath, K key, V value) {
        Map<K, List<V>> map = this.computeMap(stateFilePath);
        List<V> values = map.computeIfAbsent(key, k -> new ArrayList<>(4));
        if (values.isEmpty()) {
            values.add(value);
            map.put(key, values);
            return Optional.of(0);
        } else {
            if (values.contains(value)) {
                return Optional.empty();
            } else {
                values.add(value);
                map.put(key, values);
                return Optional.of(values.size() - 1);
            }
        }
    }

    @Override
    public synchronized Optional<Integer> replaceAndReturnIndex(String stateFilePath, K key, V value) {
        // returns `0` if value not present for key, or `empty` if already present
        Map<K, List<V>> map = this.computeMap(stateFilePath);
        if (map.containsKey(key)) {
            List<V> values = map.get(key);
            if (values.contains(value)) {
                return Optional.empty();
            } else {
                map.put(key, Collections.singletonList(value));
                return Optional.of(0);
            }
        } else {
            map.put(key, Collections.singletonList(value));
            return Optional.of(0);
        }
    }

    @Override
    public synchronized void replace(String stateFilePath, K key, List<V> value) {
        Map<K, List<V>> map = this.computeMap(stateFilePath);
        map.put(key, value);
    }

    @Override
    public synchronized boolean hasKey(String stateFilePath, K key) {
        Map<K, List<V>> map = this.computeMap(stateFilePath);
        return map.containsKey(key);
    }

    @Override
    public synchronized Map<K, List<V>> getEntries(String stateFilePath) {
        return this.computeMap(stateFilePath);
    }

    @Override
    public synchronized void remove(String stateFilePath, K key) {
        Map<K, List<V>> map = this.computeMap(stateFilePath);
        map.remove(key);
    }

    /**
     * Closes the state: state is persisted to disk before clearing the state in memory.
     */
    @Override
    public synchronized void close() {
        saveAllState();
        stateFileToMap.clear();
    }

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
    public synchronized long count(final String stateFilePath, final K key) {
        Map<K, List<V>> map = this.computeMap(stateFilePath);
        List<V> values = map.get(key);
        if (values == null) {
            return 0;
        } else {
            return values.size();
        }

    }
}
