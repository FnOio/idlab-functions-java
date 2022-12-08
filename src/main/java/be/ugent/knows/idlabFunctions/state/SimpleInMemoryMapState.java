package be.ugent.knows.idlabFunctions.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public class SimpleInMemoryMapState implements MapState {
    private final static Logger log = LoggerFactory.getLogger(SimpleInMemoryMapState.class);
    private final static Map<String, Map<String, String>> stateFileToMap = new HashMap<>();

    public synchronized String put(final String stateFilePath, final String key, final String value) {
        Map<String, String> map = stateFileToMap.computeIfAbsent(stateFilePath, mapKey -> {
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
        return map.put(key, value);
    }

    /**
     * Closes the state: state is persisted to disk before clearing the state in memory.
     */
    @Override
    public synchronized void close() {
        stateFileToMap.forEach((stateFilePath, stateMap) -> {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(stateFilePath, false))) {
                out.writeObject(stateMap);
            } catch (IOException e) {
                log.warn("Cannot save state map to {}", stateFilePath);
            }
        });
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
}
