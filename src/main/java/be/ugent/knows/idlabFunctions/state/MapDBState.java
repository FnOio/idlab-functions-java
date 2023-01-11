package be.ugent.knows.idlabFunctions.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A MapState backed by MapDB. This implementation is slower than the {@link SimpleInMemoryMapState},
 * but it handles multi-threading better and commits its state every 10 seconds to disk.
 *
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public class MapDBState implements MapState {
    private final static Logger log = LoggerFactory.getLogger(MapDBState.class);
    private final static Map<String, MapDBContainer> stateFileToMap = new ConcurrentHashMap<>();

    /**
     * Loads or creates a new state map for the given state file path.
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <br>
     * Note that the state is committed every 10 seconds to file.
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
    public String put(String stateFilePath, String key, String value) {
        MapDBContainer container = stateFileToMap.computeIfAbsent(stateFilePath, mapKey -> new MapDBContainer(stateFilePath));
        return container.put(key, value);
    }

    @Override
    public void deleteAllState() {
        synchronized (stateFileToMap) {
            stateFileToMap.forEach((stateFilePath, mapContainer) -> {
                mapContainer.close();
                File stateFile = new File(stateFilePath);
                if (!stateFile.delete()) {
                    log.warn("Could not delete map state file {}", stateFilePath);
                }
            });
            stateFileToMap.clear();
        }
    }

    @Override
    public void saveAllState() {
        stateFileToMap.forEach((stateFilePath, mapContainer) -> mapContainer.commit());
    }

    @Override
    public long count(final String stateFilePath) {
        synchronized (stateFileToMap) {
            if (stateFileToMap.containsKey(stateFilePath)) {
                return stateFileToMap.get(stateFilePath).count();
            } else {
                return 0;
            }
        }
    }

    @Override
    public void close() {
        synchronized (stateFileToMap) {
            stateFileToMap.forEach((stateFilePath, mapContainer) -> mapContainer.close());
            stateFileToMap.clear();
        }
    }
}
