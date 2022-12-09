package be.ugent.knows.idlabFunctions.state;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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

    public static Stream<Arguments> stateAndNrCombinations() {
        List<MapState> states = List.of(new SimpleInMemoryMapState(), new MapDBState());
        List<Integer> entries = List.of(10000, 100000, 1000 * 1000/*, 10 * 1000 * 1000*/);
        List<Arguments> arguments = new ArrayList<>();
        states.forEach(state ->{
            entries.forEach(entry -> arguments.add(Arguments.of(state, entry)));
        });
        return arguments.stream();
    }

    public static Stream<Arguments> states() {
        return Stream.of(
                Arguments.of(new SimpleInMemoryMapState()),
                Arguments.of(new MapDBState())
        );
    }

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testSingleThreadedState(boolean isSimple) throws Exception {
        try (MapState state = isSimple ? new SimpleInMemoryMapState() : new MapDBState()) {
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
        try (MapState state = isSimple ? new SimpleInMemoryMapState() : new MapDBState()) {
            String previous = state.put(tmpStateFile1.getPath(), "Hello", "World");
            assertEquals("do not change anymore", previous);

            // now delete everything!
            state.deleteAllState();
        }
    }

    @ParameterizedTest
    @MethodSource("states")
    public void testMultiThreadedState(MapState state) throws Exception {
        final String stateFile1 = tmpStateFile1.getPath();
        final String stateFile2 = tmpStateFile2.getPath();
        ExecutorService service = Executors.newFixedThreadPool(8);

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

    @ParameterizedTest
    @MethodSource("stateAndNrCombinations")
    public void testManyEntriesSingleThread(MapState state, int nrEntries) {
        System.out.println("state: " + state.getClass().getSimpleName() + "; nr entries: " + nrEntries / 1000 + " K");
        final String stateFileString = tmpStateFile1.getPath();
        long charsWritten = 0;
        final Random r = new Random();
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < nrEntries; i++) {
            long longValue = r.nextLong();
            String key = Long.toHexString(longValue);
            String binaryValue = Long.toString(longValue, 2);
            state.put(stateFileString, key, binaryValue);
            charsWritten += key.length();
            charsWritten += binaryValue.length();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("charsWritten = " + charsWritten);
        System.out.println("time (ms) = " + (endTime - startTime));
        state.deleteAllState();
    }

    @ParameterizedTest
    @MethodSource("stateAndNrCombinations")
    public void testManyEntriesParallel(MapState state, int nrEntries) throws InterruptedException {
        System.out.println("state: " + state.getClass().getSimpleName() + "; nr entries: " + nrEntries / 1000 + " K");
        final String stateFile1 = tmpStateFile1.getPath();
        final String stateFile2 = tmpStateFile2.getPath();
        ExecutorService service = Executors.newFixedThreadPool(8);

        final Random r = new Random();
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < nrEntries; i++) {
            service.submit(() -> {
                long longValue = r.nextInt();
                String sFile = longValue % 2 == 0 ? stateFile1 : stateFile2;
                String key = Long.toHexString(longValue);
                String binaryValue = Long.toString(longValue, 2);
                state.put(sFile, key, binaryValue);
            });
        }
        service.shutdown();
        if (!service.awaitTermination(100, TimeUnit.SECONDS)) {
            log.warn("Waiting for executor to run all tasks failed for some reason... Never mind.");
        }
        long endTime = System.currentTimeMillis();
        System.out.println("time (ms) = " + (endTime - startTime));
        state.deleteAllState();
    }

    @ParameterizedTest
    @MethodSource("states")
    public void testSaveAllState(MapState state) throws Exception {
        // first create a state by putting something in it
        String previous = state.put(tmpStateFile1.getPath(), "Hello", "World");
        assertNull(previous);

        state.saveAllState();

        assertTrue(tmpStateFile1.exists());
        state.deleteAllState();
    }

    @ParameterizedTest
    @MethodSource("states")
    public void testDeleteAllState(MapState state) {
        // first create a state by putting something in it
        String previous = state.put(tmpStateFile1.getPath(), "Hello", "World");
        assertNull(previous);

        state.deleteAllState();

        assertFalse(tmpStateFile1.exists());
        state.deleteAllState();
    }
}
