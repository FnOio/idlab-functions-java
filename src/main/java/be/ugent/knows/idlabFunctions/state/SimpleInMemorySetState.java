package be.ugent.knows.idlabFunctions.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class SimpleInMemorySetState implements SetState {
    private final static Logger log = LoggerFactory.getLogger(SimpleInMemorySetState.class);

    private final Map<String, Set<String>> stateFileToSet = new HashMap<>();

    private synchronized Set<String> computeSet(final String stateFilePath) {
        return stateFileToSet.computeIfAbsent(stateFilePath, setKey -> {
            // first check if file exists and try to load map
            File stateFile = new File(stateFilePath);
            Set<String> newSet = new HashSet<>();
            if (stateFile.exists() && stateFile.isFile() && stateFile.canRead()) {
                try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(stateFilePath)))){
                    newSet = (Set<String>)in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    log.warn("Cannot load state set from file {}. Creating empty set!", stateFilePath);
                }
            }
            return newSet;
        });
    }

    @Override
    public synchronized boolean contains(String stateFilePath, String value) {
        Set<String> set = computeSet(stateFilePath);
        return set.contains(value);
    }

    @Override
    public synchronized void saveAllState() {
        stateFileToSet.forEach((stateFilePath, stateSet) -> {
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(stateFilePath, false)))) {
                out.writeObject(stateSet);
            } catch (IOException e) {
                log.warn("Cannot save state set to {}", stateFilePath);
            }
        });
    }

    @Override
    public synchronized void deleteAllState() {
        stateFileToSet.forEach((stateFilePath, stateSet) -> {
            File stateFile = new File(stateFilePath);
            if (!stateFile.delete()) {
                log.warn("Could not delete {}", stateFilePath);
            }
        });
        stateFileToSet.clear();
    }

    @Override
    public void add(String stateFilePath, String value) {
        Set<String> set = computeSet(stateFilePath);
        set.add(value);
    }

    @Override
    public synchronized void close() {
        saveAllState();
        stateFileToSet.clear();
    }
}
