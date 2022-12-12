package be.ugent.knows.idlabFunctions.state;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A record holding a MapDB instance and the corresponding map.
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public class MapDBContainer {
    private final static Logger logger = LoggerFactory.getLogger(MapDBContainer.class);
    private final DB mapDB;
    private final HTreeMap<String, String> map;

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
                logger.warn(dbPArentFile + " doesn not exist and could not be created, creating a temporary file state.");
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

        map = mapDB.hashMap("map", Serializer.STRING, Serializer.STRING).createOrOpen();

        // make an executor service that does a commit on the mapDB every 10 seconds
        committer = Executors.newSingleThreadScheduledExecutor();
        committer.scheduleAtFixedRate(mapDB::commit, 10, 10, TimeUnit.SECONDS);
    }
    public String put(String key, String value) {
        return map.put(key, value);
    }

    public void close() {
        committer.shutdown();
        map.close();
        mapDB.close();
    }

    public void commit() {
        mapDB.commit();
    }
}
