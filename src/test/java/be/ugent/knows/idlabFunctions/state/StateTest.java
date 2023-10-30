package be.ugent.knows.idlabFunctions.state;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public static Stream<Arguments> mapStates() {
        return Stream.of(
                Arguments.of(new SimpleInMemoryMapState<String, String>()),
                Arguments.of(new SimpleInMemorySingleValueMapState<String, String>())
        );
    }

    public static Stream<Arguments> stateAndNrCombinations() {
        List<?> states = List.of(new SimpleInMemoryMapState<String, String>(), new SimpleInMemorySingleValueMapState<String, String>());
        List<Integer> entries = List.of(100, 1000, 10000, 1000000);
        List<Arguments> arguments = new ArrayList<>();
        states.forEach(state ->{
            entries.forEach(entry -> arguments.add(Arguments.of(state, entry)));
        });
        return arguments.stream();
    }

    //@BeforeEach
    @AfterEach
    @BeforeEach
    public void deleteStateFiles() {

        if (tmpStateFile1.exists()) {
            log.debug("Deleting {}", tmpStateFile1);
            if (!tmpStateFile1.delete()) {
                log.warn("Cannot delete state file {}. Ignore if test deleted state.", tmpStateFile1.getPath());
            }
        }
        if (tmpStateFile2.exists()) {
            log.debug("Deleting {}", tmpStateFile2);
            if (!tmpStateFile2.delete()) {
                log.warn("Cannot delete state file {}. Ignore if test deleted state.", tmpStateFile2.getPath());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testSingleThreadedState(boolean isSingleValueMap) throws Exception {
        try (MapState<?, String, String> state = isSingleValueMap? new SimpleInMemorySingleValueMapState<>(): new SimpleInMemoryMapState<>()) {
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
        try (MapState<?, String, String> state = isSingleValueMap? new SimpleInMemorySingleValueMapState<>(): new SimpleInMemoryMapState<>()) {
            String previous = state.put(tmpStateFile1.getPath(), "Hello", "World");
            assertEquals("do not change anymore", previous);

            // now delete everything!
            state.deleteAllState();
        }
    }

    @ParameterizedTest
    @MethodSource("mapStates")
    public void testMultiThreadedState(MapState<?, String, String> state) {
        final String stateFile1 = tmpStateFile1.getPath();
        final String stateFile2 = tmpStateFile2.getPath();

        Stream.generate(new Random()::nextInt)
                .limit(1000000)
                .parallel()
                .forEach(nr -> {
                    String sFile = nr % 2 == 0 ? stateFile1 : stateFile2;
                    String key = nr % 2 == 0 ? "Hello" : "Bye";
                    String value = nr % 2 == 0 ? "moon" : "world";
                    String oldValue = state.put(sFile, key, value);
                    assertTrue(oldValue == null || oldValue.equals("world") || oldValue.equals("moon"));
                });
    }

    @ParameterizedTest
    @MethodSource("stateAndNrCombinations")
    public void testManyEntriesSingleThread(MapState<?, String, String> state, int nrEntries) {
        System.out.println("nr entries: " + nrEntries / 1000 + " K");
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
    }

    @ParameterizedTest
    @MethodSource("stateAndNrCombinations")
    public void testManyEntriesParallel(MapState<?, String, String> state, int nrEntries) {
        System.out.println("nr entries: " + nrEntries / 1000 + " K");
        final String stateFile1 = tmpStateFile1.getPath();
        final String stateFile2 = tmpStateFile2.getPath();
        final ExecutorService service = Executors.newFixedThreadPool(8);

        long startTime = System.currentTimeMillis();
        Stream.generate(new Random()::nextLong)
                .limit(nrEntries)
                .parallel()
                .forEach(nr -> {
                    String sFile = nr % 2 == 0 ? stateFile1 : stateFile2;
                    String key = Long.toHexString(nr);
                    String binaryValue = Long.toString(nr, 2);
                    state.putAndReturnIndex(sFile, key, binaryValue);
                });
        long endTime = System.currentTimeMillis();
        System.out.println("time (ms) = " + (endTime - startTime));
    }

    @Test
    public void testPutAndReturnIndex() throws Exception {
        try (MapState<List<String>, String, String> state = new SimpleInMemoryMapState<>()) {
            Optional<Integer> indexOpt = state.putAndReturnIndex(tmpStateFile1.getPath(), "acertainkey", "a");
            assertTrue(indexOpt.isPresent());
            assertEquals(0, indexOpt.get());

            indexOpt = state.putAndReturnIndex(tmpStateFile1.getPath(), "acertainkey", "b");
            assertTrue(indexOpt.isPresent());
            assertEquals(1, indexOpt.get());

            indexOpt = state.putAndReturnIndex(tmpStateFile1.getPath(), "acertainkey", "a");
            assertFalse(indexOpt.isPresent());
        }
    }

    @Test
    public void testSaveAllState() throws Exception {
        // first create a state by putting something in it
        try (MapState<List<String>, String, String> state = new SimpleInMemoryMapState<>()) {
            String previous = state.put(tmpStateFile1.getPath(), "Hello", "World");
            assertNull(previous);

            state.saveAllState();

            assertTrue(tmpStateFile1.exists());
        }
    }

    @Test
    public void testDeleteAllState() throws Exception {
        try (MapState<List<String>, String, String> state = new SimpleInMemoryMapState<>()) {
            // first create a state by putting something in it
            String previous = state.put(tmpStateFile1.getPath(), "Hello", "World");
            assertNull(previous);

            state.deleteAllState();

            assertFalse(tmpStateFile1.exists());
            state.deleteAllState();
        }
    }

    @Test
    public void timeSaveState() throws Exception {
        final int nrEntries = 1000000;
        final String stateFile1 = tmpStateFile1.getPath();
        final ExecutorService service = Executors.newFixedThreadPool(8);

            long startTime = 0;

            try (final MapState<List<String>, String, String> state = new SimpleInMemoryMapState<>()) {

            // populate the state with a lot of random stuff
            final Random r = new Random();
            for (int i = 0; i < nrEntries; i++) {
                service.submit(() -> {
                    long longValue = r.nextInt();
                    String key = Long.toHexString(longValue);
                    String binaryValue = Long.toString(longValue, 2);
                    state.putAndReturnIndex(stateFile1, key, binaryValue);
                });
            }
            service.shutdown();
            if (!service.awaitTermination(100, TimeUnit.SECONDS)) {
                log.warn("Waiting for executor to run all tasks failed for some reason... Never mind.");
            }

            // start timing; the try-with-resources will auto-close and save the state.
            System.out.println("Saving state...");
            startTime = System.currentTimeMillis();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("time (ms) = " + (endTime - startTime));
    }

    @Test
    public void testSetState() throws Exception {
        try (SetState<String> state = new SimpleInMemorySetState<>()) {
            state.add(tmpStateFile1.getPath(), "Hello");
            state.add(tmpStateFile1.getPath(), "world");
            assertTrue(state.contains(tmpStateFile1.getPath(), "Hello"));
            assertTrue(state.contains(tmpStateFile1.getPath(), "world"));
        }
    }
}
