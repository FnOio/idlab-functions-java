package be.ugent.knows.idlabFunctions.state;

import org.junit.jupiter.api.Test;

import java.io.File;

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
}
