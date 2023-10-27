package be.ugent.knows.idlabFunctions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LDESGenerationTests {
    private final static Logger log = LoggerFactory.getLogger(LDESGenerationTests.class);

    private static final String STATE_FILE = new File(System.getProperty("java.io.tmpdir"), "state_file").getPath();

    @AfterEach
    @BeforeEach
    public void deleteStateFiles() {
        File stateFile = new File(STATE_FILE);
        if (stateFile.exists()) {
            log.debug("Deleting {}", STATE_FILE);
            if (!stateFile.delete()) {
                log.warn("Cannot delete state file {}. Ignore if test deleted state.", STATE_FILE);
            }
        }
    }

    @AfterEach
    public void cleanUp() {
        IDLabFunctions.resetState();
    }

    @Test
    public void skipGenerateUniqueIRI() {
        String iri = "http://example.com/sensor1/";
        String value = "pressure=5";
        boolean isUnique = false;

        String firstUniqueIRI = IDLabFunctions.generateUniqueIRI(iri, value, isUnique, STATE_FILE);
        assertNotNull(firstUniqueIRI);
        String generated_iri = IDLabFunctions.generateUniqueIRI(iri, value, isUnique, STATE_FILE);
        assertNull(generated_iri);
    }

    @Test
    public void generateUniqueIRI() {
        String iri = "http://example.com/sensor2/";
        String value = null;
        boolean isUnique = true;

        String generated_iri = IDLabFunctions.generateUniqueIRI(iri, value, isUnique, STATE_FILE);
        assertEquals(iri, generated_iri);
    }

    @Test
    public void generateSecondUniqueIRI() {
        String iri = "http://example.com/sensor2/";

        final String generated_iri_1 = IDLabFunctions.generateUniqueIRI(iri, "a=1", false, STATE_FILE);
        final String generated_iri_2 = IDLabFunctions.generateUniqueIRI(iri, "a=2", false, STATE_FILE);
        assertNotEquals(generated_iri_1, generated_iri_2);

        final String generated_iri_3 = IDLabFunctions.generateUniqueIRI(iri, "a=1", false, STATE_FILE);
        assertNull(generated_iri_3);
    }

    @Test
    public void generateUniqueIRIWithDate() {

        String iri = "http://example.com/sensor2/";
        String value = "pressure=5";
        boolean isUnique = false;

        String generated_iri = IDLabFunctions.generateUniqueIRI(iri, value, isUnique, STATE_FILE);
        assertNotNull(generated_iri);
        assertTrue(generated_iri.contains(iri));
    }

    @Test
    public void explicitCreate() {
        String iri = "http://example.com/sensor2/";

        String generated_iri = IDLabFunctions.explicitCreate(iri, STATE_FILE);
        assertNotNull(generated_iri);
        assertTrue(generated_iri.contains(iri));

        generated_iri = IDLabFunctions.explicitCreate(iri, STATE_FILE);
        assertNull(generated_iri);
    }

    @Test
    public void implicitCreate() {
        String iri = "http://example.com/sensor2/";

        String generated_iri = IDLabFunctions.implicitCreate(iri, STATE_FILE);
        assertNotNull(generated_iri);
        assertTrue(generated_iri.contains(iri));

        generated_iri = IDLabFunctions.implicitCreate(iri, STATE_FILE);
        assertNull(generated_iri);
    }

    @Test
    public void explicitUpdate() {
        String iri = "http://example.com/sensor2/";

        String generated_iri = IDLabFunctions.explicitUpdate(iri, STATE_FILE);
        assertNotNull(generated_iri);
        assertTrue(generated_iri.contains(iri));

        generated_iri = IDLabFunctions.explicitUpdate(iri, STATE_FILE);
        assertNull(generated_iri);
    }

    @Test
    public void implicitUpdate() {
        String iri = "http://example.com/sensor2/";
        String value = "pressure=5";

        String generated_iri = IDLabFunctions.implicitUpdate(iri, value, STATE_FILE);
        assertNull(generated_iri);

        value = "pressure=6";
        generated_iri = IDLabFunctions.implicitUpdate(iri, value, STATE_FILE);
        assertNotNull(generated_iri);
        assertTrue(generated_iri.contains(iri));

        generated_iri = IDLabFunctions.implicitUpdate(iri, value, STATE_FILE);
        assertNull(generated_iri);
    }

    @Test
    public void implicitUpdateMultiple() {
        String iri = "http://example.com/sensor2/";
        String value = "pressure=5&timestamp=1";

        String generated_iri = IDLabFunctions.implicitUpdate(iri, value, STATE_FILE);
        assertNull(generated_iri);

        value = "pressure=5&timestamp2";
        generated_iri = IDLabFunctions.implicitUpdate(iri, value, STATE_FILE);
        assertNotNull(generated_iri);
        assertTrue(generated_iri.contains(iri));

        generated_iri = IDLabFunctions.implicitUpdate(iri, value, STATE_FILE);
        assertNull(generated_iri);
    }

    @Test
    public void explicitDelete() {
        String iri = "http://example.com/sensor2/";

        String generated_iri = IDLabFunctions.explicitDelete(iri, STATE_FILE);
        assertNotNull(generated_iri);
        assertTrue(generated_iri.contains(iri));

        generated_iri = IDLabFunctions.explicitDelete(iri, STATE_FILE);
        assertNull(generated_iri);
    }

    @Test
    public void implicitDelete() {
        String iri1 = "http://example.com/sensor1/";
        String iri2 = "http://example.com/sensor2/";

        /* Mark 2 members as seen */
        assertNull(IDLabFunctions.implicitDelete(iri1, STATE_FILE));
        IDLabFunctions.close();
        assertNull(IDLabFunctions.implicitDelete(iri2, STATE_FILE));
        IDLabFunctions.close();

        /* Process all deletions, none should be marked as deleted */
        assertNull(IDLabFunctions.implicitDelete(IDLabFunctions.MAGIC_MARKER, STATE_FILE));
        IDLabFunctions.close();

        /* Marker 1 as seen */
        assertNull(IDLabFunctions.implicitDelete(iri1, STATE_FILE));
        IDLabFunctions.close();

        /* Process all deletions, iri2 should be marked as deleted */
        List<String> generated = IDLabFunctions.implicitDelete(IDLabFunctions.MAGIC_MARKER, STATE_FILE);
        IDLabFunctions.close();
        assertNotNull(generated);
        assertEquals(1, generated.size());
        assertEquals(iri2, generated.get(0));
    }

    @Test
    public void testSaveState() {
        String iri = "http://example.com/sensor2/";
        String value = "pressure=5";
        boolean isUnique = false;

        String generated_iri = IDLabFunctions.generateUniqueIRI(iri, value, isUnique, STATE_FILE);
        assertNotNull(generated_iri);

        IDLabFunctions.saveState();

        assertTrue(new File(STATE_FILE).exists());
    }

    @Test
    public void testDefaultStateFile() throws IOException {
        String iri = "http://example.com/sensor2/";
        String value = "pressure=5";
        boolean isUnique = false;

        String generated_iri = IDLabFunctions.generateUniqueIRI(iri, value, isUnique, null);
        assertNotNull(generated_iri);
        IDLabFunctions.close();

        // check state dir
        final File stateFile = new File(System.getProperty("java.io.tmpdir"), "unique_iri_state");
        assertTrue(stateFile.exists());

        // remove it
        if (!stateFile.delete()) {
            throw new IOException(stateFile + "should be deleted.");
        }
    }

    @Test
    public void testTmpStateFile() throws IOException {
        String iri = "http://example.com/sensor2/";
        String value = "pressure=5";
        boolean isUnique = false;

        String generated_iri = IDLabFunctions.generateUniqueIRI(iri, value, isUnique, "__tmp");
        assertNotNull(generated_iri);
        IDLabFunctions.close();

        // check state dir
        final File stateFile = new File(System.getProperty("java.io.tmpdir"), "unique_iri_state");
        assertTrue(stateFile.exists());

        // remove it
        if (!stateFile.delete()) {
            throw new IOException(stateFile + "should be deleted.");
        }
    }

    @Test
    public void testWorkingDirState() throws IOException {
        String iri = "http://example.com/sensor2/";
        String value = "pressure=5";
        boolean isUnique = false;

        String generated_iri = IDLabFunctions.generateUniqueIRI(iri, value, isUnique, "__working_dir");
        assertNotNull(generated_iri);
        IDLabFunctions.close();

        // check state dir
        final File stateFile = new File(System.getProperty("user.dir"), "unique_iri_state");
        assertTrue(stateFile.exists());

        // remove it
        if (!stateFile.delete()) {
            throw new IOException(stateFile + "should be deleted.");
        }
    }
}
