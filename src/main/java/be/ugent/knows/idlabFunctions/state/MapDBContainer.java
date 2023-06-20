package be.ugent.knows.idlabFunctions.state;

import org.mapdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A record holding a MapDB instance and the corresponding map.
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public class MapDBContainer {
    private final static Logger logger = LoggerFactory.getLogger(MapDBContainer.class);
    private final DB mapDB;
    private final ScheduledExecutorService committer;

    /**
     * Creates a MapDBContainer instance. If {@code dbFilePath} is given, it is backed by a memory mapped mapdb.
     * If {@code dbFilePath} is {@code null} or cannot be created, a temporary file is created and deleted when closed.
     * @param dbFilePath    The path to the mapdb file. If it exists, the file is loaded. If it doesn't, a new map is created.
     */
    public MapDBContainer(final String dbFilePath) {

        DBMaker.Maker dbMaker;
        if (dbFilePath != null) {
            File dbPArentFile = new File(dbFilePath).getParentFile();
            if (!dbPArentFile.exists() && !dbPArentFile.mkdirs()) {
                logger.warn(dbPArentFile + " does not exist and could not be created, creating a temporary file state.");
                dbMaker = DBMaker.tempFileDB();
            } else {
                dbMaker = DBMaker.fileDB(dbFilePath);
            }
        } else {
            logger.debug("dbFilePath is null, creating a temporary state.");
            dbMaker = DBMaker.tempFileDB();
        }

        mapDB = dbMaker.fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .make();

        // make an executor service that does a commit on the mapDB every 10 seconds
        committer = Executors.newSingleThreadScheduledExecutor();
        committer.scheduleAtFixedRate(mapDB::commit, 10, 10, TimeUnit.SECONDS);

        addShutDownHook();
    }
    public String put(String key, String value) {
        IndexTreeList<String> values = mapDB.indexTreeList(key, Serializer.STRING).createOrOpen();
        if (values.isEmpty()) {
            values.add(value);
            return null;
        } else {
            if (values.contains(value)) {
                return values.get(values.size() - 1);
            } else {
                String returnValue = values.get(values.size() - 1);
                values.add(value);
                return returnValue;
            }
        }
    }

    public Optional<Integer> putAndReturnIndex(String key, String value) {
        IndexTreeList<String> values = mapDB.indexTreeList(key, Serializer.STRING).createOrOpen(); //mapDB.hashSet(key, Serializer.STRING).createOrOpen();
        if (values.isEmpty()) {
            values.add(value);
            return Optional.of(0);
        } else {
            if (values.contains(value)) {
                return Optional.empty();
            } else {
                values.add(value);
                return Optional.of(values.size() - 1);
            }
        }
    }

    public boolean hasKey(String key) {
        IndexTreeList<String> values = mapDB.indexTreeList(key, Serializer.STRING).createOrOpen(); //mapDB.hashSet(key, Serializer.STRING).createOrOpen();

        if (values.isEmpty())
            return false;

        return true;
    }

    public Map<String, List<String>> getEntries() {
        Map <String, List<String>> entries = new HashMap<>();
        for (String key: mapDB.getAllNames()) {
            IndexTreeList<String> values = mapDB.indexTreeList(key, Serializer.STRING).createOrOpen();
            entries.put(key, values);
        }
        return entries;
    }

    public long count(String key) {
        Object valuesObject = mapDB.get(key);
        if (valuesObject == null) {
            return 0;
        } else {
            List<String> values = Collections.unmodifiableList((List<String>) valuesObject);
            return values.size();
        }
    }

    public void replace(String key, List<String> value) {
        IndexTreeList<String> values = mapDB.indexTreeList(key, Serializer.STRING).createOrOpen();
        values.clear();
        values.addAll(value);
    }

    public void close() {
        committer.shutdown();
        mapDB.close();
    }

    public void commit() {
        mapDB.commit();
    }

    // add a ShutDownHook to close everything when JVM stops
    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.debug("Running shutdown hook; closing mapdb");
            close();
        }));
    }

    public void remove(String key) {
        IndexTreeList<String> values = mapDB.indexTreeList(key, Serializer.STRING).createOrOpen();
        values.clear();
    }
}
