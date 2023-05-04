package be.ugent.knows.idlabFunctions;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class IDLabFunctionsTest {

    @Test
    public void stringContainsOtherStringTrue() {
        String str = "Finding a needle in a haystack";
        String otherStr = "needle";
        String delimiter = " ";
        assertTrue(IDLabFunctions.stringContainsOtherString(str, otherStr, delimiter));
    }

    @Test
    public void stringContainsOtherStringFalse() {
        String str = "What you are looking for is not here";
        String otherStr = "needle";
        String delimiter = " ";
        assertFalse(IDLabFunctions.stringContainsOtherString(str, otherStr, delimiter));
    }

    @Test
    public void listContainsElementTrue() {
        List<String> list = Arrays.asList("apple", "banana", "lemon", "orange");
        String str = "lemon";
        assertTrue(IDLabFunctions.listContainsElement(list, str));
    }

    @Test
    public void listContainsElementFalse() {
        List<String> list = Arrays.asList("apple", "banana", "lemon", "orange");
        String str = "pear";
        assertFalse(IDLabFunctions.listContainsElement(list, str));
    }


    @Test
    @Disabled
    public void dbpediaSpotlight() {
//        String endpoint = "http://193.190.127.195/dbpedia-spotlight/rest";
//        List<String> entities = IDLabFunctions.dbpediaSpotlight("Barack Obama", endpoint);
//        ArrayList<String> expected = new ArrayList<>();
//        expected.add("http://dbpedia.org/resource/Barack_Obama");
//
//        assertThat(entities, CoreMatchers.is(expected));
//
//        entities = IDLabFunctions.dbpediaSpotlight("", endpoint);
//        expected = new ArrayList<>();
//
//        assertThat(entities, CoreMatchers.is(expected));
//
//        entities = IDLabFunctions.dbpediaSpotlight("a", endpoint);
//        expected = new ArrayList<>();
//
//        assertThat(entities, CoreMatchers.is(expected));
    }

    @Test
    public void trueCondition() {
        Object result = IDLabFunctions.trueCondition("true", "hello");
        assertEquals("hello", result);

        result = IDLabFunctions.trueCondition("false", "hello");
        assertNull(result);

        result = IDLabFunctions.trueCondition("test", "hello");
        assertNull(result);
    }

    @Test
    public void decideTrue() {
        String input = "foo";
        String expected = "foo";
        String value = "success!";
        assertEquals(value, IDLabFunctions.decide(input, expected, value));
    }

    @Test
    public void decideFalse() {
        String input = "foo";
        String expected = "bar";
        String value = "success!";
        assertNull(IDLabFunctions.decide(input, expected, value));
    }

    @Test
    public void isNullTrue() {
        String input = null;
        assertTrue(IDLabFunctions.isNull(input));
    }

    @Test
    public void isNullFalse() {
        String input = "Hello";
        assertFalse(IDLabFunctions.isNull(input));
    }

    @Test
    public void getMIMEType() {
        String result = IDLabFunctions.getMIMEType("test.csv");
        assertEquals("text/csv", result);

        result = IDLabFunctions.getMIMEType("test.json");
        assertEquals("application/json", result);
    }

    @Test
    public void readFileValidPath() {
        String path = "student.csv";
        String result = IDLabFunctions.readFile(path);
        assertNotNull(result);
        assertTrue(result.contains("Id,Name,Comment,Class"));
    }

    @Test
    public void readFileInvalidPath() {
        String path = "rml-fno-test-cases/does_not_exist.txt";
        String result = IDLabFunctions.readFile(path);
        assertNull(result);
    }

    @Test
    public void random() {
        String result = IDLabFunctions.random();
        try {
            UUID uuid = UUID.fromString(result);
        } catch (IllegalArgumentException e) {
            fail();
        }
    }

    @Test
    public void toUpperCaseURL() {
        String noProtocol = "www.example.com";
        String withProtocol = "http://www.example.com";

        String result = IDLabFunctions.toUpperCaseURL(noProtocol);
        assertEquals("HTTP://WWW.EXAMPLE.COM", result);
        result = IDLabFunctions.toUpperCaseURL(withProtocol);
        assertEquals("HTTP://WWW.EXAMPLE.COM", result);
    }

    @Test
    public void inRange() {
        assertTrue(IDLabFunctions.inRange(3.0, 1.0, 5.0));
        assertFalse(IDLabFunctions.inRange(3.0, 1.0, 3.0));
        assertTrue(IDLabFunctions.inRange(3.0, 1.0, null));
        assertTrue(IDLabFunctions.inRange(3.0, null, 5.0));
        assertTrue(IDLabFunctions.inRange(3.0, null, null));
        assertFalse(IDLabFunctions.inRange(null, null, null));
    }

    @Test
    public void slugify() {
        String result = IDLabFunctions.slugify("Ben De Mééster");
        assertEquals("ben-de-meester", result);
    }

    @Test
    public void normalizeDateWithLang() {
        String input1 = "20220121";
        String format1 = "yyyyMMdd";
        assertEquals("2022-01-21", IDLabFunctions.normalizeDateWithLang(input1, format1, "en"));

        String input2 = "01 April 22";
        // String format2 = "dd LLLL uu";	// This does not work on Java 8!
        String format2 = "dd MMMM uu";
        assertEquals("2022-04-01", IDLabFunctions.normalizeDateWithLang(input2, format2, "en"));

        assertNull(IDLabFunctions.normalizeDateWithLang("rubbish", "yodelahiti", "en"));

        // will fail because "April" is no French
        assertNull(IDLabFunctions.normalizeDateWithLang(input2, format2, "fr"));

        String input3 = "01-avr.-22";   // yes, French abbreviations need a '.' !
        String format3 = "dd-MMM-yy";
        assertEquals("2022-04-01", IDLabFunctions.normalizeDateWithLang(input3, format3, "fr"));
    }

    @Test
    public void normalizeDate() {
        String input1 = "20220121";
        String format1 = "yyyyMMdd";
        assertEquals("2022-01-21", IDLabFunctions.normalizeDate(input1, format1));

        assertNull(IDLabFunctions.normalizeDate("rubbish", "yodelahiti"));

    }

    @Test
    public void normalizeDateTimeWithLang() {
        String input1 = "20220121 7 14 33";
        String format1 = "yyyyMMdd H m s";
        assertEquals("2022-01-21T07:14:33", IDLabFunctions.normalizeDateTimeWithLang(input1, format1, "en"));
    }

    @Test
    public void normalizeDateTime() {
        String input1 = "20200521 17 14 33";
        String format1 = "yyyyMMdd H m s";
        assertEquals("2020-05-21T17:14:33", IDLabFunctions.normalizeDateTime(input1, format1));

        // 20220124T09:36:04,yyyyMMdd'THH:mm:ss
        String input2 = "20220124T09:36:04";
        String format2 = "yyyyMMdd'T'HH:mm:ss";
        assertEquals("2022-01-24T09:36:04", IDLabFunctions.normalizeDateTime(input2, format2));

        String input3 = "01-Apr-20 9u4";
        String format3 = "dd-MMM-yy H'u'm";
        assertEquals("2020-04-01T09:04:00", IDLabFunctions.normalizeDateTime(input3, format3));

    }

    public static class LDESGenerationTests {

        private static final String STATE_FILE = new File(System.getProperty("java.io.tmpdir"), "tmpState1").getPath();


        @AfterEach
        public void cleanUp() {
            IDLabFunctions.resetState();
        }

        @Test
        public void skipGenerateUniqueIRI() {
            String template = "http://example.com/sensor1/";
            String value = "pressure=5";
            boolean isUnique = false;

            String firstUniqueIRI = IDLabFunctions.generateUniqueIRI(template, value, isUnique, STATE_FILE);
            assertNotNull(firstUniqueIRI);
            String generated_iri = IDLabFunctions.generateUniqueIRI(template, value, isUnique, STATE_FILE);
            assertNull(generated_iri);
        }


        @Test
        public void generateUniqueIRI() {
            String template = "http://example.com/sensor2/";
            String value = null;
            boolean isUnique = true;

            String generated_iri = IDLabFunctions.generateUniqueIRI(template, value, isUnique, STATE_FILE);
            assertEquals(template, generated_iri);

        }

        @Test
        public void generateSecondUniqueIRI() {
            String template = "http://example.com/sensor2/";

            final String generated_iri_1 = IDLabFunctions.generateUniqueIRI(template, "a=1", false, STATE_FILE);
            final String generated_iri_2 = IDLabFunctions.generateUniqueIRI(template, "a=2", false, STATE_FILE);
            assertNotEquals(generated_iri_1, generated_iri_2);

            final String generated_iri_3 = IDLabFunctions.generateUniqueIRI(template, "a=1", false, STATE_FILE);
            assertNull(generated_iri_3);
        }

        @Test
        public void generateUniqueIRIWithDate() {

            String template = "http://example.com/sensor2/";
            String value = "pressure=5";
            boolean isUnique = false;

            String generated_iri = IDLabFunctions.generateUniqueIRI(template, value, isUnique, STATE_FILE);
            assertNotNull(generated_iri);
            assertTrue(generated_iri.contains(template));
        }

        @Test
        public void testSaveState() {
            String template = "http://example.com/sensor2/";
            String value = "pressure=5";
            boolean isUnique = false;

            String generated_iri = IDLabFunctions.generateUniqueIRI(template, value, isUnique, STATE_FILE);
            assertNotNull(generated_iri);

            IDLabFunctions.saveState();

            assertTrue(new File(STATE_FILE).exists());
        }

        @Test
        public void testDefaultStateFile() throws IOException {
            String template = "http://example.com/sensor2/";
            String value = "pressure=5";
            boolean isUnique = false;

            String generated_iri = IDLabFunctions.generateUniqueIRI(template, value, isUnique, null);
            assertNotNull(generated_iri);

            // check state dir
            final File stateFile = new File(System.getProperty("java.io.tmpdir"), "unique_iri_state");
            assertTrue(stateFile.exists());
            IDLabFunctions.close();

            // remove it
            if (!stateFile.delete()) {
                throw new IOException(stateFile + "should be deleted.");
            }
        }

        @Test
        public void testTmpStateFile() throws IOException {
            String template = "http://example.com/sensor2/";
            String value = "pressure=5";
            boolean isUnique = false;

            String generated_iri = IDLabFunctions.generateUniqueIRI(template, value, isUnique, "__tmp");
            assertNotNull(generated_iri);

            // check state dir
            final File stateFile = new File(System.getProperty("java.io.tmpdir"), "unique_iri_state");
            assertTrue(stateFile.exists());
            IDLabFunctions.close();

            // remove it
            if (!stateFile.delete()) {
                throw new IOException(stateFile + "should be deleted.");
            }
        }

        @Test
        public void testWorkingDirState() throws IOException {
            String template = "http://example.com/sensor2/";
            String value = "pressure=5";
            boolean isUnique = false;

            String generated_iri = IDLabFunctions.generateUniqueIRI(template, value, isUnique, "__working_dir");
            assertNotNull(generated_iri);

            // check state dir
            final File stateFile = new File(System.getProperty("user.dir"), "unique_iri_state");
            assertTrue(stateFile.exists());
            IDLabFunctions.close();

            // remove it
            if (!stateFile.delete()) {
                throw new IOException(stateFile + "should be deleted.");
            }
        }
    }

    @Test
    public void testsortWatchedProperties() {
        assertEquals("a=1&b=2", IDLabFunctions.sortWatchedProperties("b=2&a=1"));
    }

    @Test
    public void lookup() throws CsvValidationException, IOException {
        String searchString = "A";
        String inputFile = "src/test/resources/class.csv";
        Integer fromColumn = 0;
        Integer toColumn = 1;
        assertEquals("Class A", IDLabFunctions.lookup(searchString, inputFile, fromColumn, toColumn));

        String delimiter = ",";
        assertEquals("Class A", IDLabFunctions.lookupWithDelimiter(searchString, inputFile, fromColumn, toColumn, delimiter));

        searchString = "Class B";
        assertNull(IDLabFunctions.lookup(searchString, inputFile, fromColumn, toColumn));

        searchString = "Class B";
        fromColumn = 2;
        assertNull(IDLabFunctions.lookup(searchString, inputFile, fromColumn, toColumn));

        searchString = "B";
        fromColumn = 0;
        inputFile = "src/test/resources/classB.csv";
        delimiter = ";";
        assertEquals("Class B", IDLabFunctions.lookupWithDelimiter(searchString, inputFile, fromColumn, toColumn, delimiter));
    }

    private String name = "Alexander";
    private final String comment = "A&B";
    private String classType = "B";
    private String inputFile = "src/test/resources/students.csv";



    @Test
    public void simpleMultipleLookup() throws CsvValidationException, IOException {

        assertEquals("2",
                IDLabFunctions.multipleLookup(new ArrayList<>(Arrays.asList(name, comment)),
                        new ArrayList<>(Arrays.asList(1, 2)),
                        inputFile, 0, ","));

        assertEquals(name,
                IDLabFunctions.multipleLookup(new ArrayList<>(Arrays.asList(comment, classType)),
                        new ArrayList<>(Arrays.asList(2, 3)),
                        inputFile, 1, ","));

        name = "Stella";
        classType = "A";
        assertEquals(name,
                IDLabFunctions.multipleLookup(new ArrayList<>(Arrays.asList("7", comment, classType)),
                        new ArrayList<>(Arrays.asList(0,2,3)),
                        inputFile,1, ","));

        name = "Stella";
        assertEquals("7",
                IDLabFunctions.multipleLookup(null, name, comment, null, null, null,null, 1, 2,
                        null, null, null, inputFile,0, ","));

        anotherFileToHashmap();

        assertEquals(71,IDLabFunctions.getMultipleLookupStateSet().size());

    }

    @Test
    public void simpleMultipleLookupSizeOfMapCheck() throws CsvValidationException, IOException {

        assertEquals("2",
                IDLabFunctions.multipleLookup(new ArrayList<>(Arrays.asList(name, comment)),
                        new ArrayList<>(Arrays.asList(1, 2)),
                        inputFile, 0, ","));
        assertEquals(23,IDLabFunctions.getMultipleLookupStateSet().size());

    }

    @Test
    public void simpleMultipleLookupSizeOfCacheCheck() throws CsvValidationException, IOException {

        assertEquals("2",
                IDLabFunctions.multipleLookup(new ArrayList<>(Arrays.asList(name, comment)),
                        new ArrayList<>(Arrays.asList(1, 2)),
                        inputFile, 0, ","));
        assertEquals(IDLabFunctions.getCache().size(),IDLabFunctions.getMultipleLookupStateSet().size());

    }
        @Test
    public void twoFilesWithSameSearchParameters() throws CsvValidationException, IOException {
        name = "Alexander";
        classType = "B";

        assertEquals("2",
                IDLabFunctions.multipleLookup(new ArrayList<>(Arrays.asList(name, comment)),
                        new ArrayList<>(Arrays.asList(1, 2)),
                        inputFile, 0, ","));

        inputFile =  "src/test/resources/studentsCopy.csv";

        assertEquals("2a",
                IDLabFunctions.multipleLookup(new ArrayList<>(Arrays.asList(name, comment)),
                        new ArrayList<>(Arrays.asList(1, 2)),
                        inputFile, 0, ","));
    }


    @Test
    public void columnIsOverLimit() throws CsvValidationException, IOException {
        assertNull(IDLabFunctions.multipleLookup(new ArrayList<>(Collections.singletonList(name)),
                new ArrayList<>(Collections.singletonList(2)),
                inputFile, 6, ","));
    }

    @Test
    public void incorrectSizesOfIndexesAndValues() throws CsvValidationException, IOException {

        assertNull(IDLabFunctions.multipleLookup(new ArrayList<>(Collections.singletonList(name)),
                new ArrayList<>(Arrays.asList(2, 3)),
                inputFile, 0, ","));

    }

    @Test
    public void nullCheck() throws CsvValidationException, IOException {
        
        assertThrows(IllegalArgumentException.class, () -> IDLabFunctions.multipleLookup(null, new ArrayList<>(Collections.singletonList(2)), inputFile, 3, ","));
        assertThrows(IllegalArgumentException.class, () -> IDLabFunctions.multipleLookup(new ArrayList<>(Collections.singletonList(name)), null, inputFile, 3, ","));
        assertNull(IDLabFunctions.multipleLookup(new ArrayList<>(Collections.singletonList(name)), new ArrayList<>(Collections.singletonList(1)), inputFile, null, ","));

        assertNull(IDLabFunctions.multipleLookup("3", name, comment, null, null, null, null, 1, 2,
                null, null, null, inputFile, 0, ","));
    }

    public void anotherFileToHashmap() throws CsvValidationException, IOException {
        name = "Venus";
        classType = "A";
        inputFile =  "src/test/resources/studentsCopy.csv";

        assertEquals("1a",
                IDLabFunctions.multipleLookup(new ArrayList<>(Arrays.asList(name, classType)),
                        new ArrayList<>(Arrays.asList(1, 3)),
                        inputFile, 0, ","));

    }

    @Test
    public void testConcatSequenceNormal() {
        String result = IDLabFunctions.concatSequence(Arrays.asList("een", "twee"), ",");
        assertEquals("een,twee", result);
    }

    @Test
    public void testConcatSequenceEmptySequence() {
        String result = IDLabFunctions.concatSequence(Collections.emptyList(), ",");
        assertEquals("", result);
    }

    @Test
    public void testConcatSequenceNullDelimiter() {
        String result = IDLabFunctions.concatSequence(Arrays.asList("een", "twee"), null);
        assertEquals("eentwee", result);
    }

}
