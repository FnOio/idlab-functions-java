package be.ugent.knows.idlabFunctions.state;

import org.junit.jupiter.api.Test;

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

    @Test
    public void testSingleThreadedState() {
        final String tempStateDir = new File(System.getProperty("java.io.tmpdir"), "tmpState").getPath();
        final File tempStateDirFile = new File(tempStateDir);

        try (PersistentMapState state = new PersistentMapState()) {
            // first create a state by putting something in it
            String previous = state.putIfAbsent(tempStateDir, "Hello", "World");
            assertNull(previous);

            assertTrue(tempStateDirFile.exists());

            // now the value doesn't get updated because there is already a value
            String newPrevious = state.putIfAbsent(tempStateDir, "Hello", "new world");
            assertEquals("World", newPrevious);

            // now close the states, but keep the states on disk
            state.close();

            // put new value
            newPrevious = state.put(tempStateDir, "Hello", "old world");
            assertEquals("World", newPrevious);

            // see if new value is put
            newPrevious = state.putIfAbsent(tempStateDir, "Hello", "do not change anymore");
            assertEquals("old world", newPrevious);

            // clear all states
            state.resetStates();
            newPrevious = state.putIfAbsent(tempStateDir, "Hello", "you");
            assertNull(newPrevious);

        } finally {
            if (tempStateDirFile.exists()) {
                assertTrue(tempStateDirFile.delete());
            }
        }
    }

    @Test
    public void testMultiThreadedState() {
        final File stateFile1 = new File(System.getProperty("java.io.tmpdir"), "tmpState1");
        final File stateFile2 = new File(System.getProperty("java.io.tmpdir"), "tmpState2");
        ExecutorService service = Executors.newFixedThreadPool(8);

        try (PersistentMapState state = new PersistentMapState()) {
            final Random r = new Random();
            for (int i = 0; i < 10000; i++) {
                service.submit(() -> {
                    int nr = r.nextInt(2);
                    String sFile = nr % 2 == 0 ? stateFile1.getPath() : stateFile2.getPath();
                    String key = nr % 2 == 0 ? "Hello" : "Bye";
                    String value = nr % 2 == 0 ? "moon" : "world";
                    String oldValue = state.put(sFile, key, value);
                    assertTrue(oldValue == null || oldValue.equals("world") || oldValue.equals("moon"));
                });
            }
            service.shutdown();
            service.awaitTermination(100, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (stateFile1.exists()) {
                stateFile1.delete();
            }
            if (stateFile2.exists()) {
                stateFile2.delete();
            }
        }
    }

    @Test
    public void test1Mentries() {
        final File stateFile = new File(System.getProperty("java.io.tmpdir"), "tmpState");
        final String stateFileString = stateFile.getPath();
        long charsWritten = 0;
        try (PersistentMapState state = new PersistentMapState()) {
            final Random r = new Random();
            for (int i = 0; i < 1000000; i++) {
                long longValue = r.nextLong();
                String key = Long.toHexString(longValue);
                String binalyValue = Long.toString(longValue, 2);
                state.put(stateFileString, key, binalyValue);
                charsWritten += key.length();
                charsWritten += binalyValue.length();
            }
            System.out.println("charsWritten = " + charsWritten);
        } finally {
            if (stateFile.exists()) {
                stateFile.delete();
            }
        }
    }
}
