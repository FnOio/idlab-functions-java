package be.ugent.knows.idlabFunctions.state;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

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
    private final DB mapDB;
    private final HTreeMap<String, String> map;

    private final ScheduledExecutorService committer;

    public MapDBContainer(final String dbFilePath) {
        mapDB = DBMaker
                .fileDB(dbFilePath)
                .fileMmapEnableIfSupported()
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
