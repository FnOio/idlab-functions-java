package be.ugent.knows.idlabFunctions.state;

import net.openhft.chronicle.hash.ChronicleHash;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public class PersistentMapState implements AutoCloseable {
    private final static Map<String, ChronicleMap<String, String>> stateFileToMap = new ConcurrentHashMap<>(); // map file -> key -> value

    /**
     * Loads or creates a new state map for the given state file path.
     * If the specified key is not already associated
     * with a value in that map, associates it with the given value.
     * This is equivalent to, for this {@code map}:
     * <pre> {@code
     * if (!map.containsKey(key))
     *   return map.put(key, value);
     * else
     *   return map.get(key);}</pre>
     *
     * except that the action is performed atomically.
     *
     * @param stateFilePath The path of this state map's persistance file.
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     */
    public synchronized String putIfAbsent(final String stateFilePath, final String key, final String value) {
        // TODO: synchronization only needs to happen for a given stateFilePath, but the same String constants are not necessarily the same Objects.
        ConcurrentMap<String, String> map = createStateMapIfAbsent(stateFilePath, key, value);
        return map.putIfAbsent(key, value);
    }

    /**
     * Loads or creates a new state map for the given state file path.
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
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
    public String put(final String stateFilePath, final String key, final String value) {
        ConcurrentMap<String, String> map = createStateMapIfAbsent(stateFilePath, key, value);
        return map.put(key, value);
    }

    private ConcurrentMap<String, String> createStateMapIfAbsent(final String stateFilePath, final String averageKey, final String averageValue) {
        return stateFileToMap.computeIfAbsent(stateFilePath, mapKey -> {
            ChronicleMap<String, String> cMap;
            try {
                cMap = ChronicleMapBuilder
                        .of(String.class, String.class)
                        .averageKey(averageKey)
                        .averageValue(averageValue)
                        .entries(1000000)
                        .createPersistedTo(new File(stateFilePath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return cMap;
        });
    }

    /**
     * Closes ALL state maps
     */
    public void close() {
        stateFileToMap.values().forEach(ChronicleHash::close);
        stateFileToMap.clear();
    }

    /**
     * Resets ALL state maps
     */
    public void resetStates () {
       stateFileToMap.values().forEach(Map::clear);
    }
}
