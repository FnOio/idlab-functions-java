package be.ugent.knows.idlabFunctions.state;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public class StateTest {
    public final static Logger log = LoggerFactory.getLogger(StateTest.class);
    private final static File tmpStateFile1 = new File(System.getProperty("java.io.tmpdir"), "tmpState1");
    private final static File tmpStateFile2 = new File(System.getProperty("java.io.tmpdir"), "tmpState2");

    //@BeforeEach
    @AfterEach
    public void deleteStateFiles() {
        if (tmpStateFile1.exists()) {
            if (!tmpStateFile1.delete()) {
                log.warn("Cannot delete state file {}. Ignore if test deleted state.", tmpStateFile1.getPath());
            }
        }
        if (tmpStateFile2.exists()) {
            if (!tmpStateFile2.delete()) {
                log.warn("Cannot delete state file {}. Ignore if test deleted state.", tmpStateFile2.getPath());
            }
        }
    }
    @Test
    public void testSingleThreadedState() throws Exception {
        try (MapState state = new SimpleInMemoryMapState()) {
            // first create a state by putting something in it
            String previous = state.put(tmpStateFile1.getPath(), "Hello", "World");
            assertNull(previous);

            // now the value doesn't get updated because there is already a value
            String newPrevious = state.put(tmpStateFile1.getPath(), "Hello", "new world");
            assertEquals("World", newPrevious);

            // now close the states, but keep the states on disk
            state.close();

            // put new value
            newPrevious = state.put(tmpStateFile1.getPath(), "Hello", "old world");
            assertEquals("new world", newPrevious);

            // see if new value is put
            newPrevious = state.put(tmpStateFile1.getPath(), "Hello", "do not change anymore");
            assertEquals("old world", newPrevious);
        }
        // at this point the state closes

        // now re-load state
        try (SimpleInMemoryMapState state = new SimpleInMemoryMapState()) {
            String previous = state.put(tmpStateFile1.getPath(), "Hello", "World");
            assertEquals("do not change anymore", previous);

            // now delete everything!
            state.deleteAllState();
        }
    }

    @Test
    public void testMultiThreadedState() throws Exception {
        final String stateFile1 = tmpStateFile1.getPath();
        final String stateFile2 = tmpStateFile2.getPath();
        ExecutorService service = Executors.newFixedThreadPool(8);

        try (MapState state = new SimpleInMemoryMapState()) {
            final Random r = new Random();
            for (int i = 0; i < 10000; i++) {
                service.submit(() -> {
                    int nr = r.nextInt(2);
                    String sFile = nr % 2 == 0 ? stateFile1 : stateFile2;
                    String key = nr % 2 == 0 ? "Hello" : "Bye";
                    String value = nr % 2 == 0 ? "moon" : "world";
                    String oldValue = state.put(sFile, key, value);
                    assertTrue(oldValue == null || oldValue.equals("world") || oldValue.equals("moon"));
                });
            }
            service.shutdown();
            if (!service.awaitTermination(100, TimeUnit.SECONDS)) {
                log.warn("Waiting for executor to run all tasks failed for some reason... Never mind.");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {10000L, 100000L, 1000L * 1000L})
    public void testManyEntries(long nrEntries) throws Exception {
        final String stateFileString = tmpStateFile1.getPath();
        long charsWritten = 0;
        try (MapState state = new SimpleInMemoryMapState()) {
            final Random r = new Random();
            for (int i = 0; i < nrEntries; i++) {
                long longValue = r.nextLong();
                String key = Long.toHexString(longValue);
                String binalyValue = Long.toString(longValue, 2);
                state.put(stateFileString, key, binalyValue);
                charsWritten += key.length();
                charsWritten += binalyValue.length();
            }
            System.out.println("charsWritten = " + charsWritten);
        } 
    }
}
