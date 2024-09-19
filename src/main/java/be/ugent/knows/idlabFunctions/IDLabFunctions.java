package be.ugent.knows.idlabFunctions;

import be.ugent.knows.idlabFunctions.state.*;
import be.ugent.knows.util.Cache;
import be.ugent.knows.util.SearchParameters;
import be.ugent.knows.util.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.slugify.Slugify;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class IDLabFunctions {

    private static final Logger logger = LoggerFactory.getLogger(IDLabFunctions.class);
    private final static SetState<String> IMPLICIT_CREATE_STATE = new SimpleInMemorySetState<>();
    private final static MapState<String, String, String> IMPLICIT_UPDATE_STATE = new SimpleInMemorySingleValueMapState<>();
    private final static MapState<Boolean, String, Boolean> IMPLICIT_DELETE_STATE = new SimpleInMemorySingleValueMapState<>();
    private final static SetState<String> EXPLICIT_CREATE_STATE = new SimpleInMemorySetState<>();
    private final static SetState<String> EXPLICIT_UPDATE_STATE = new SimpleInMemorySetState<>();
    private final static SetState<String> EXPLICIT_DELETE_STATE = new SimpleInMemorySetState<>();
    private final static MapState<List<String>, String, String> UNIQUE_IRI_STATE = new SimpleInMemoryMapState<>();
    private final static SetState<String> UNIQUE_CREATE_IRI_STATE = new SimpleInMemorySetState<>();
    private final static MapState<List<String>, String, String> UNIQUE_UPDATE_IRI_STATE = new SimpleInMemoryMapState<>();

    /* Some valriables used by create, update and delete functions */
    public final static String MAGIC_MARKER = "!@#$%^&()_+";
    public final static String MAGIC_MARKER_ENCODED = "%21%40%23%24%25%5E%26%28%29_%2B";

    private final static boolean IMPLICIT_DELETE_SEEN_ID = true;
    private final static boolean IMPLICIT_DELETE_NOT_SEEN_ID = false;

    private final static Map<String, String> STATE_FILE_PATH_CACHE = new HashMap<>();

    // used by the lookup function
    private static final Map<String, String> LOOKUP_STATE_MAP = new HashMap<>();
    private static final Map<SearchParameters, String> MULTIPLE_LOOKUP_STATE_MAP = new HashMap<>();
    private static final Map<String, List<String[]>> CACHE = new HashMap<>();

    /**
     * Resolve the state dir path. If not supplied, a path is constructed from a given property `ifState` if set,
     * or the system's temporary directory if not set.
     * The working directory can be used if the path equals to __working_dir or a temporary directory if the path
     * equals to __tmp. If a specific path is provided, it is just returned
     * @param stateDirPathStr   String representation of the file path in which the state of the function
     *                          will be stored. It can have four kinds of values:
     *                          <ul>
     *                          <li>{@code __tmp}: The state is kept in a file {@code state_file} in a
     *                          temporary directory determined by the OS. </li>
     *                          <li>{@code __working_dir} The state is kept in a file {@code unique_iri_state} in the
     *                          user's current working directory.</li>
     *                          <li>The path to the directory where state is / will be kept.</li>
     *                          <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                          the behaviour is the same as if it were {@code __tmp}</li>
     *                          </ul>
     * @param state_file        The file where state is kept within {@code stateDirPathStr}.
     * @return the resolved state dir path
     */
    static String resolveStateDirPath(final String stateDirPathStr, final String state_file) {
        final String actualStateFilePathStr;
        final String checkedStateDirPathStr;

        // Make stateDirPathStr not null
        if (stateDirPathStr == null || stateDirPathStr.isEmpty()) {
            logger.debug("stateDirPathStr = NULL. Trying to read ifState property.");
            checkedStateDirPathStr = System.getProperty("ifState", "__tmp");
        } else {
            checkedStateDirPathStr = stateDirPathStr;
        }

        // Check if in cache, and return result if so
        final String cacheKey = checkedStateDirPathStr + '/' + state_file;
        final String result = STATE_FILE_PATH_CACHE.get(cacheKey);
        if (result != null) {
            return result;
        }

        // Not in cache, do complete resolving.
        logger.debug("checkedStateDirPathStr = '{}'", checkedStateDirPathStr);
        if (checkedStateDirPathStr.equals("__tmp")) {
            actualStateFilePathStr = new File(System.getProperty("java.io.tmpdir"), state_file).getPath();
        } else if (checkedStateDirPathStr.equals("__working_dir")) {
            actualStateFilePathStr = new File(System.getProperty("user.dir"), state_file).getPath();
        } else {
            File stateDir = new File(checkedStateDirPathStr);
            if (!stateDir.exists()) {
                if (stateDir.mkdirs()) {
                    logger.debug("Created new state file directory '{}'", stateDir);
                } else {
                    logger.warn("Could not create new state directory {}! Using system temporary directory.", stateDir);
                    stateDir = new File(System.getProperty("java.io.tmpdir"));
                }
            }
            actualStateFilePathStr = new File(stateDir, state_file).getPath();
        }
        logger.debug("actualStateFilePathStr = '{}'", actualStateFilePathStr);
        STATE_FILE_PATH_CACHE.put(cacheKey, actualStateFilePathStr);
        return actualStateFilePathStr;
    }

    public static Map<SearchParameters, String> getMultipleLookupStateSet(){
        return Cache.getMultipleLookupStateMap();
    }
    public static Map<SearchParameters, String> getCache(){
        return Cache.getCache();
    }
    public static boolean stringContainsOtherString(String str, String otherStr, String delimiter) {
        String[] split = str.split(delimiter);
        List<String> list = Arrays.asList(split);

        return list.contains(otherStr);
    }

    public static boolean listContainsElement(List list, String str) {
        if (list != null) {
            return list.contains(str);
        } else {
            return false;
        }
    }

    public static List<String> dbpediaSpotlight(String text, String endpoint) {
        if (!text.equals("")) {
            try {
                URL url = new URL(endpoint + "/annotate?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Accept", "application/json");
                con.setInstanceFollowRedirects(true);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                con.disconnect();

                Object document = Configuration.defaultConfiguration().jsonProvider().parse(content.toString());
                return JsonPath.parse(document).read("$.Resources[*].@URI");
            } catch (PathNotFoundException e) {
                // that means no result was found, so that is fine
                logger.info(e.getMessage(), e);
            } catch (Exception e) {
                // that probably means smth is wrong with the DBpedia Spotlight endpoint, so that is fine: log and continue
                logger.warn(e.getMessage(), e);
            }
        }

        return new ArrayList<>();
    }

    public static String trueCondition(String bool, String value) {
        if (bool == null || !bool.equals("true")) {
            return null;
        } else {
            return value;
        }
    }

    public static String decide(String input, String expected, String result) {
        if (input != null && input.equals(expected)) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Tests whether a given input is null or not.
     *
     * @param input String to evaluate
     * @return whether it's null or not
     */
    public static boolean isNull(String input) {
        return input == null;
    }

    public static String getMIMEType(String filename) {
        if (filename == null) {
            return null;
        } else {
            HashMap<String, String> map = new HashMap<>();

            // Put elements in the hashMap
            map.put("csv", "text/csv");
            map.put("json", "application/json");
            map.put("xml", "application/xml");
            map.put("nt", "application/n-triples");
            map.put("ttl", "text/turtle");
            map.put("nq", "application/n-quads");
            map.put("sql", "application/sql");

            int lastIndexOfPoint = filename.lastIndexOf('.');
            String extension = lastIndexOfPoint < 0 ? "" : filename.substring(Math.min(lastIndexOfPoint + 1, filename.length()));

            return map.getOrDefault(extension, null);
        }
    }

    public static String readFile(String path) {
        try {
            logger.debug(Utils.getFile(path).toString());
            return Utils.fileToString(Utils.getFile(path));
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
            return null;
        }
    }

    public static String random() {
        return UUID.randomUUID().toString();

    }

    public static String toUpperCaseURL(String test) {
        String upperTest = test.toUpperCase();
        if (!upperTest.startsWith("HTTP://")) {
            upperTest = "HTTP://" + upperTest;
        }
        return upperTest;
    }

    /**
     * Tests whether a certain number is in a certain range.
     * Everything is optional:
     * - function returns false when testNumber is null
     * - function only tests from constraint when to is null
     * - function only tests to constraint when from is null
     * - function returns true when from and to are null.
     *
     * @param testNumber The number put under the test. Optional (function returns false when is null)
     * @param from       The number from where (inclusive)
     * @param to         The number until where (exclusive)
     * @return whether it's in range or not
     */
    public static boolean inRange(Double testNumber, Double from, Double to) {
        if (testNumber == null) {
            return false;
        }
        if (from == null && to == null) {
            return true;
        }

        if (from == null) {
            return testNumber < to;
        }
        if (to == null) {
            return testNumber >= from;
        }
        return testNumber >= from && testNumber < to;
    }

    /**
     * Convert a string to its slugified equivalent.
     *
     * @param str The String to slugify
     * @return the slugified string. Returns null if the input was also null.
     */
    public static String slugify(String str) {
        if (str != null) {
            Slugify slg = Slugify.builder().build();
            return slg.slugify(str);
        }
        return null;
    }

    // TODO The functions below are currently untested and undefined as idlab-fn functions.
    // (They however do no belong to GREL functions either)

    // TODO check whether this is the right place for this
    public static boolean isSet(String valueParameter) {
        return (valueParameter != null && !valueParameter.isEmpty());
    }

    // TODO check whether this is the right place for this
    public static boolean booleanMatch(String valueParameter, String regexParameter) {
        return valueParameter.matches(regexParameter);
    }

    ///////////////////////////////
    // Date formatting functions //
    ///////////////////////////////
    // TODO check whether this is the right place for this

    /**
     * Returns a given date(time) string as an ISO-8601 formatted date(time) string.
     *
     * @param dateStr     Input string representing a parsable date or dateTime, e.g. "01 April 22"
     * @param pattern     DateTime format pattern used to parse the given dateStr as defined in {@link DateTimeFormatter}, e.g. "dd LLLL uu"
     * @param language    The language of dateStr, as defined in {@link Locale}
     * @param includeTime If <code>true</code>, include the time part in the output.
     * @return A normalized date string in the ISO-8601 format uuuu-MM-dd (xs:date) or uuuu-MM-ddTHH:mm:ss,
     * or null if parsing the input fails.
     */
    private static String normalizeDateTimeStr(String dateStr, String pattern, String language, boolean includeTime) {
        try {
            Locale locale = new Locale(language);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);
            if (includeTime) {
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            } else {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return date.toString();
            }
        } catch (Throwable e) {
            logger.error("{}; format pattern: \"{}\", input: \"{}\", language: \"{}\"", e.getMessage(), pattern, dateStr, language);
            return null;
        }
    }

    /**
     * Returns `dateStr` as a normalized xs:date string, using `pattern` as current date form.
     *
     * @param dateStr  Input string representing a parsable date, e.g. "01 April 22"
     * @param pattern  Date format pattern used to parse the given dateStr as defined in {@link DateTimeFormatter}, e.g. "dd LLLL uu"
     * @param language The language of dateStr, as defined in {@link Locale}
     * @return A normalized date string in the ISO-8601 format uuuu-MM-dd (xs:date), or null if parsing the input fails.
     */
    public static String normalizeDateWithLang(String dateStr, String pattern, String language) {
        return normalizeDateTimeStr(dateStr, pattern, language, false);
    }

    /**
     * Returns `dateStr` as a normalized xs:date string, using `pattern` as current date form. It uses the language of
     * the current locale of the JVM to parse certain input strings, like names of months.
     *
     * @param dateStr Input string representing a parsable date, e.g. "01 April 22"
     * @param pattern Date format pattern used to parse the given dateStr as defined in {@link SimpleDateFormat}, e.g. "dd LLL y"
     * @return A normalized date string in the ISO-8601 format uuuu-MM-dd (xs:date), or null if parsing the input fails.
     */
    public static String normalizeDate(String dateStr, String pattern) {
        return normalizeDateWithLang(dateStr, pattern, Locale.getDefault().getLanguage());
    }

    /**
     * Returns `dateTimeStr` as a normalized xs:date string, using `pattern` as current datetime form.
     *
     * @param dateTimeStr Input string representing a parsable datetime, e.g. "01 April 22T11:44:00"
     * @param pattern     Date format pattern used to parse the given dateStr as defined in {@link DateTimeFormatter}, e.g. "dd LLLL uuTHH:mm:ss"
     * @param language    The language of dateStr, as defined in {@link Locale}
     * @return A normalized date string in the ISO-8601 format uuuu-MM-ddTHH:mm:ss (xs:datetime), or null if parsing the input fails.
     */
    public static String normalizeDateTimeWithLang(String dateTimeStr, String pattern, String language) {
        return normalizeDateTimeStr(dateTimeStr, pattern, language, true);
    }

    /**
     * Returns `dateTimeStr` as a normalized xs:date string, using `pattern` as current datetime form.
     * It uses the language of the current locale of the JVM to parse certain input strings, like names of months.
     *
     * @param dateTimeStr Input string representing a parsable datetime, e.g. "01 April 22T11:44:00"
     * @param pattern     Date format pattern used to parse the given dateStr as defined in {@link DateTimeFormatter}, e.g. "dd LLLL uuTHH:mm:ss"
     * @return A normalized date string in the ISO-8601 format uuuu-MM-ddTHH:mm:ss (xs:datetime), or null if parsing the input fails.
     */
    public static String normalizeDateTime(String dateTimeStr, String pattern) {
        return normalizeDateTimeWithLang(dateTimeStr, pattern, Locale.ENGLISH.getLanguage());
    }

    public static void saveState() {
        IMPLICIT_CREATE_STATE.saveAllState();
        IMPLICIT_UPDATE_STATE.saveAllState();
        IMPLICIT_DELETE_STATE.saveAllState();
        EXPLICIT_CREATE_STATE.saveAllState();
        EXPLICIT_UPDATE_STATE.saveAllState();
        EXPLICIT_DELETE_STATE.saveAllState();
        UNIQUE_IRI_STATE.saveAllState();
        UNIQUE_CREATE_IRI_STATE.saveAllState();
        UNIQUE_UPDATE_IRI_STATE.saveAllState();
    }

    public static void resetState() {
        IMPLICIT_CREATE_STATE.deleteAllState();
        IMPLICIT_UPDATE_STATE.deleteAllState();
        IMPLICIT_DELETE_STATE.deleteAllState();
        EXPLICIT_CREATE_STATE.deleteAllState();
        EXPLICIT_UPDATE_STATE.deleteAllState();
        EXPLICIT_DELETE_STATE.deleteAllState();
        UNIQUE_IRI_STATE.deleteAllState();
        UNIQUE_CREATE_IRI_STATE.deleteAllState();
        UNIQUE_UPDATE_IRI_STATE.deleteAllState();
    }

    public static void close() {
        try {
            IMPLICIT_CREATE_STATE.close();
            IMPLICIT_UPDATE_STATE.close();
            IMPLICIT_DELETE_STATE.close();
            EXPLICIT_CREATE_STATE.close();
            EXPLICIT_UPDATE_STATE.close();
            EXPLICIT_DELETE_STATE.close();
            UNIQUE_IRI_STATE.close();
            UNIQUE_CREATE_IRI_STATE.close();
            UNIQUE_UPDATE_IRI_STATE.close();
            LOOKUP_STATE_MAP.clear();
            STATE_FILE_PATH_CACHE.clear();
        } catch (Exception e) {
            logger.warn("Cannot close state.", e);
        }
    }

    /**
     * The generation of the IRI depends on the value of the watched properties.
     * If any of the watched properties changes in value or gets dropped, a unique IRI will be
     * generated. Otherwise, null String will be returned.
     * In order to check if the watched properties have changed, a file state is written to keep track of
     * previously seen property values.
     *
     * @param iri                  The IRI from which a unique IRI should be generated. If it is guaranteed to be unique
     *                             (set by the {@code isUnique} parameter) then this function just returns the template.
     *                             If not, a unique string will be appended to the returned IRI.
     * @param isUnique             A flag to indicate whether the given template already creates a unique IRI. If set to
     *                             {@code true}, this function returns the value of the {@code template} parameters.
     *                             If set to {@code false}, then {@code watchedValueTemplate} is checked: if it has the
     *                             same value as the previous call then there's no update and this function returns {@code null}.
     *                             If the value of {@code watchedValueTemplate} differs from the previous call, then
     *                             this function returns an IRI composed of the template + a unique string.
     * @param watchedValueTemplate The template string containing the key-value pairs of properties being watched. Only
     *                             used if the template is not unique (set by the {@code isUnique} parameter).
     * @param stateDirPathStr       String representation of the file path in which the state of the function
     *                              will be stored. It can have four kinds of values:
     *                              <ul>
     *                              <li>{@code __tmp}: The state is kept in a file {@code unique_iri_state} in a
     *                              temporary directory determined by the OS. </li>
     *                              <li>{@code __working_dir} The state is kept in a file {@code unique_iri_state} in the
     *                              user's current working directory.</li>
     *                              <li>The path to the directory where state is / will be kept.</li>
     *                              <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                              the behaviour is the same as if it were {@code __tmp}</li>
     *                              </ul>
     * @return A unique IRI will be generated from the provided "template" string by appending a unique string
     * if possible. Otherwise, null is returned.
     */
    public static String generateUniqueIRI(String iri, String watchedValueTemplate, Boolean isUnique, String stateDirPathStr) {
        if (isUnique == null || !isUnique) {
            /* Required parameter */
            if (watchedValueTemplate == null) {
                logger.error("Watched value template is a required parameter but was not provided");
                return null;
            }

            final String actualStateDirPathStr = IDLabFunctions.resolveStateDirPath(stateDirPathStr, "unique_iri_state");
            final String watchedPropertyString = sortWatchedProperties(watchedValueTemplate);
            Optional<Integer> indexOpt = UNIQUE_IRI_STATE.putAndReturnIndex(actualStateDirPathStr, iri, watchedPropertyString);
            return indexOpt
                    .map(integer -> iri + '#' + Long.toString(integer, Character.MAX_RADIX))
                    .orElse(null);
        }

        return iri;
    }

    /**
     * Generates a unique versioned IRI of an entity that is seen for the first time.
     *
     * @param iri                  The IRI from which a unique IRI should be generated. If it is guaranteed to be unique
     *                             (set by the {@code isUnique} parameter) then this function just returns the template.
     *                             If not, a unique string will be appended to the returned IRI.
     * @param isUnique             A flag to indicate whether the given template already creates a unique IRI. If set to
     *                             {@code true}, this function returns the value of the {@code template} parameters.
     *                             If set to {@code false}, then {@code watchedValueTemplate} is checked: if it has the
     *                             same value as the previous call then there's no update and this function returns {@code null}.
     *                             If the value of {@code watchedValueTemplate} differs from the previous call, then
     *                             this function returns an IRI composed of the template + a unique string.
     * @param stateDirPathStr       String representation of the file path in which the state of the function
     *                              will be stored. It can have four kinds of values:
     *                              <ul>
     *                              <li>{@code __tmp}: The state is kept in a file {@code unique_iri_state} in a
     *                              temporary directory determined by the OS. </li>
     *                              <li>{@code __working_dir} The state is kept in a file {@code unique_iri_state} in the
     *                              user's current working directory.</li>
     *                              <li>The path to the directory where state is / will be kept.</li>
     *                              <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                              the behaviour is the same as if it were {@code __tmp}</li>
     *                              </ul>
     * @return A unique IRI will be generated from the provided "template" string by appending a unique string
     * if possible. Otherwise, null is returned.
     */
    public static String createUniqueIRI(String iri, Boolean isUnique, String stateDirPathStr) {
        if (isUnique == null || !isUnique) {

            final String actualStateDirPathStr = IDLabFunctions.resolveStateDirPath(stateDirPathStr, "unique_create_iri_state");

            if(!UNIQUE_CREATE_IRI_STATE.contains(actualStateDirPathStr, iri)) {
                UNIQUE_CREATE_IRI_STATE.add(actualStateDirPathStr, iri);
                return iri + "#0";
            } else {
                return null;
            }
        }

        return iri;
    }

    /**
     * Generates a unique versioned IRI of an entity that is being updated.
     * The generation of the IRI depends on the value of the watched properties.
     * If any of the watched properties changes in value or gets dropped, a unique IRI will be
     * generated. Otherwise, null String will be returned.
     * In order to check if the watched properties have changed, a file state is written to keep track of
     * previously seen property values.
     *
     * @param iri                  The IRI from which a unique IRI should be generated. If it is guaranteed to be unique
     *                             (set by the {@code isUnique} parameter) then this function just returns the template.
     *                             If not, a unique string will be appended to the returned IRI.
     * @param isUnique             A flag to indicate whether the given template already creates a unique IRI. If set to
     *                             {@code true}, this function returns the value of the {@code template} parameters.
     *                             If set to {@code false}, then {@code watchedValueTemplate} is checked: if it has the
     *                             same value as the previous call then there's no update and this function returns {@code null}.
     *                             If the value of {@code watchedValueTemplate} differs from the previous call, then
     *                             this function returns an IRI composed of the template + a unique string.
     * @param watchedValueTemplate The template string containing the key-value pairs of properties being watched. Only
     *                             used if the template is not unique (set by the {@code isUnique} parameter).
     * @param stateDirPathStr       String representation of the file path in which the state of the function
     *                              will be stored. It can have four kinds of values:
     *                              <ul>
     *                              <li>{@code __tmp}: The state is kept in a file {@code unique_iri_state} in a
     *                              temporary directory determined by the OS. </li>
     *                              <li>{@code __working_dir} The state is kept in a file {@code unique_iri_state} in the
     *                              user's current working directory.</li>
     *                              <li>The path to the directory where state is / will be kept.</li>
     *                              <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                              the behavior is the same as if it were {@code __tmp}</li>
     *                              </ul>
     * @return A unique IRI will be generated from the provided "template" string by appending a unique string
     * if possible. Otherwise, null is returned.
     */
    public static String updateUniqueIRI(String iri, String watchedValueTemplate, Boolean isUnique, String stateDirPathStr) {
        if (isUnique == null || !isUnique) {
            /* Required parameter */
            if (watchedValueTemplate == null) {
                logger.error("Watched value template is a required parameter but was not provided");
                return null;
            }

            final String actualStateDirPathStr = IDLabFunctions.resolveStateDirPath(stateDirPathStr, "unique_update_iri_state");
            final String watchedPropertyString = sortWatchedProperties(watchedValueTemplate);

            if(UNIQUE_UPDATE_IRI_STATE.hasKey(actualStateDirPathStr, iri)) {
                /* Return a new version of the canonical IRI if any change is detected over the watched properties */
                Optional<Integer> indexOpt = UNIQUE_UPDATE_IRI_STATE.putAndReturnIndex(actualStateDirPathStr, iri, watchedPropertyString);
                return indexOpt
                        .map(integer -> iri + '#' + Long.toString(integer, Character.MAX_RADIX))
                        .orElse(null);
            } else {
                /* IRI not in state, cannot be modified yet. Insert it */
                List<String> watchedProperties = new ArrayList<>();
                watchedProperties.add(watchedPropertyString);
                UNIQUE_UPDATE_IRI_STATE.replace(actualStateDirPathStr, iri, watchedProperties);
                return null;
            }
        }

        return iri;
    }

    /*
     * Detect implicit created members by checking if their IRI exists in the state or not.
     *
     * @param iri                  The IRI template from which an IRI should be generated.
     * @param stateDirPathStr      String representation of the file path in which the state of the function
     *                             will be stored. It can have four kinds of values:
     *                             <ul>
     *                             <li>{@code __tmp}: The state is kept in a file {@code implicit_create_state} in a
     *                             temporary directory determined by the OS. </li>
     *                             <li>{@code __working_dir} The state is kept in a file {@code implicit_create_state} in the
     *                             user's current working directory.</li>
     *                             <li>The path to the directory where state is / will be kept.</li>
     *                             <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                             the behaviour is the same as if it were {@code __tmp}</li>
     *                             </ul>
     * @return The IRI is returned if the member was created, otherwise null.
     */
    public static String implicitCreate(String iri, String stateDirPathStr) {
        return create(iri, stateDirPathStr, LDES_MEMBER_CREATE_TYPE.implicit);
    }

    /**
     * Detect explicit created members by checking if their IRI exists in the state or not.
     *
     * @param iri                  The IRI template from which an IRI should be generated.
     * @param stateDirPathStr      String representation of the file path in which the state of the function
     *                             will be stored. It can have four kinds of values:
     *                             <ul>
     *                             <li>{@code __tmp}: The state is kept in a file {@code implicit_create_state} in a
     *                             temporary directory determined by the OS. </li>
     *                             <li>{@code __working_dir} The state is kept in a file {@code implicit_create_state} in the
     *                             user's current working directory.</li>
     *                             <li>The path to the directory where state is / will be kept.</li>
     *                             <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                             the behaviour is the same as if it were {@code __tmp}</li>
     *                             </ul>
     * @return The IRI is returned if the member was created, otherwise null.
     */
    public static String explicitCreate(String iri, String stateDirPathStr) {
        return create(iri, stateDirPathStr, LDES_MEMBER_CREATE_TYPE.explicit);
    }

    private enum LDES_MEMBER_CREATE_TYPE {
        implicit, explicit
    }

    private static String create(String iri, String stateDirPathStr, LDES_MEMBER_CREATE_TYPE create_type) {
        if (iri == null || iri.contains(MAGIC_MARKER) || iri.contains(MAGIC_MARKER_ENCODED))
            return null;

        String stateFilePath = "";
        SetState<String> state = null;
        switch (create_type) {
            case explicit -> {
                stateFilePath = "explicit_create_state";
                state = EXPLICIT_CREATE_STATE;
            }
            case implicit -> {
                stateFilePath = "implicit_create_state";
                state = IMPLICIT_CREATE_STATE;
            }
        }
        final String actualStateDirPathStr = IDLabFunctions.resolveStateDirPath(stateDirPathStr, stateFilePath);

        /* IRI in state, cannot be added anymore */
        if (state.contains(actualStateDirPathStr, iri))
            return null;
            /* IRI not in state, add it and return it. */
        else {
            state.add(actualStateDirPathStr, iri);
            return iri;
        }

    }

    /**
     * Detect implicit modified members by checking if their IRI exists in the state or not and if their properties
     * were modified.
     *
     * @param iri                  The IRI template from which an IRI should be generated.
     * @param watchedValueTemplate The template string containing the key-value pairs of properties being watched. Only
     *                             used if the template is not unique (set by the {@code isUnique} parameter).
     * @param stateDirPathStr      String representation of the file path in which the state of the function
     *                             will be stored. It can have four kinds of values:
     *                             <ul>
     *                             <li>{@code __tmp}: The state is kept in a file {@code implicit_update_state} in a
     *                             temporary directory determined by the OS. </li>
     *                             <li>{@code __working_dir} The state is kept in a file {@code implicit_create_state} in the
     *                             user's current working directory.</li>
     *                             <li>The path to the directory where state is / will be kept.</li>
     *                             <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                             the behaviour is the same as if it were {@code __tmp}</li>
     *                             </ul>
     * @return The IRI is returned if the member was updated, otherwise null.
     */
    public static String implicitUpdate(String iri, String watchedValueTemplate, String stateDirPathStr) {
        /* Required parameter */
        if (watchedValueTemplate == null) {
            logger.error("Watched value template is a required parameter but was not provided");
            return null;
        }

        if (iri == null || iri.contains(MAGIC_MARKER) || iri.contains(MAGIC_MARKER_ENCODED))
            return null;

        final String actualStateDirPathStr = IDLabFunctions.resolveStateDirPath(stateDirPathStr, "implicit_update_state");
        final String watchedPropertyString = sortWatchedProperties(watchedValueTemplate);

        /* IRI not in state, cannot be modified yet. Insert it */
        if (!IMPLICIT_UPDATE_STATE.hasKey(actualStateDirPathStr, iri)) {
            IMPLICIT_UPDATE_STATE.replace(actualStateDirPathStr, iri, watchedPropertyString);
            return null;
        }

        /* Return IRI if the value is new, otherwise return NULL */
        Optional<Integer> index = IMPLICIT_UPDATE_STATE.replaceAndReturnIndex(actualStateDirPathStr, iri, watchedPropertyString);
        return index.isEmpty()? null: iri;
    }

    /**
     * Detect explicit modified members by checking if their IRI exists in the state or not and if their properties
     * were modified.
     *
     * @param iri                  The IRI template from which an IRI should be generated.
     * @param stateDirPathStr      String representation of the file path in which the state of the function
     *                             will be stored. It can have four kinds of values:
     *                             <ul>
     *                             <li>{@code __tmp}: The state is kept in a file {@code implicit_update_state} in a
     *                             temporary directory determined by the OS. </li>
     *                             <li>{@code __working_dir} The state is kept in a file {@code implicit_create_state} in the
     *                             user's current working directory.</li>
     *                             <li>The path to the directory where state is / will be kept.</li>
     *                             <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                             the behaviour is the same as if it were {@code __tmp}</li>
     *                             </ul>
     * @return The IRI is returned if the member was updated, otherwise null.
     */
    public static String explicitUpdate(String iri, String stateDirPathStr) {

        if (iri == null || iri.contains(MAGIC_MARKER) || iri.contains(MAGIC_MARKER_ENCODED))
            return null;

        /*
         * We don't need to check if the IRI was already created because these are unique for each update as the
         * data source will provide explicitly which members are updated. We only need to filter out duplicates
         * if we pull the same data from the data source
         */

        final String actualStateDirPathStr = IDLabFunctions.resolveStateDirPath(stateDirPathStr, "explicit_update_state");

        /* IRI already in state, cannot be modified anymore */
        if (EXPLICIT_UPDATE_STATE.contains(actualStateDirPathStr, iri)) {
            return null;
        } else {
            EXPLICIT_UPDATE_STATE.add(actualStateDirPathStr, iri);
            return iri;
        }
    }

    /**
     * Detect implicit deleted members by checking if their IRI is not present anymore in the current version.
     *
     * @param iri                  The IRI template from which an IRI should be generated.
     *                             If not, a unique string will be appended to the returned IRI.
     * @param stateDirPathStr      String representation of the file path in which the state of the function
     *                             will be stored. It can have four kinds of values:
     *                             <ul>
     *                             <li>{@code __tmp}: The state is kept in a file {@code implicit_delete_state} in a
     *                             temporary directory determined by the OS. </li>
     *                             <li>{@code __working_dir} The state is kept in a file {@code implicit_delete_state} in the
     *                             user's current working directory.</li>
     *                             <li>The path to the directory where state is / will be kept.</li>
     *                             <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                             the behaviour is the same as if it were {@code __tmp}</li>
     *                             </ul>
     * @return List of IRIs is returned of deleted members. An empty list indicates no deletions.
     */
    public static List<String> implicitDelete(String iri, String stateDirPathStr) {

        if (iri == null)
            return null;

        List<String> deletedIRIs = new ArrayList<>();
        final String actualStateDirPathStr = IDLabFunctions.resolveStateDirPath(stateDirPathStr, "implicit_delete_state");

        /* Process deletions when marker found */
        List<String> notSeen = new ArrayList<>();
        if (iri.contains(MAGIC_MARKER) || iri.contains(MAGIC_MARKER_ENCODED)) {
            /* Iterate over each entry we may or may not have seen */
            if (logger.isDebugEnabled()) {
                logger.debug("MAGIC MARKER");
                logger.debug("Entries: {}", IMPLICIT_DELETE_STATE.getEntries(actualStateDirPathStr).size());
            }
            for (Map.Entry<String, Boolean> entry : IMPLICIT_DELETE_STATE.getEntries(actualStateDirPathStr).entrySet()) {
                boolean value = entry.getValue();
                if (logger.isDebugEnabled()) logger.debug("IRI: {}: value: {}", entry.getKey(), value);

                String key = entry.getKey();
                if (value != IMPLICIT_DELETE_SEEN_ID) {
                    deletedIRIs.add(key);
                    if (logger.isDebugEnabled()) logger.debug("Haven't seen: {} since value {}", key, value);
                /*
                 * If we have seen the entry, mark it unseen for the next time we have to check for deletions,
                 * but we never want to insert IRIs with the marker in that triggered the check, so skip those
                 */
                } else {
                    notSeen.add(key);
                }
            }

            /* Remove the entry from the state as it is deleted */
            for (String key: deletedIRIs)
                IMPLICIT_DELETE_STATE.remove(actualStateDirPathStr, key);

            for (String key: notSeen) {
                IMPLICIT_DELETE_STATE.put(actualStateDirPathStr, key, IMPLICIT_DELETE_NOT_SEEN_ID);
            }

            /* Return NULL when list is empty to avoid triggering any Triples Map */
            return deletedIRIs.isEmpty() ? null : deletedIRIs;
        /* Mark IRI as seen */
        } else {
            if (logger.isDebugEnabled()) logger.debug("Marking as seen: {}", iri);
            /* Insert the IRI into the state and mark it as seen */
            IMPLICIT_DELETE_STATE.put(actualStateDirPathStr, iri, IMPLICIT_DELETE_SEEN_ID);
            return null;
        }
    }

    /**
     * Detect explicit deleted members by checking if their IRI is not present anymore in the current version.
     *
     * @param iri                  The IRI template from which an IRI should be generated.
     * @param stateDirPathStr      String representation of the file path in which the state of the function
     *                             will be stored. It can have four kinds of values:
     *                             <ul>
     *                             <li>{@code __tmp}: The state is kept in a file {@code implicit_delete_state} in a
     *                             temporary directory determined by the OS. </li>
     *                             <li>{@code __working_dir} The state is kept in a file {@code implicit_delete_state} in the
     *                             user's current working directory.</li>
     *                             <li>The path to the directory where state is / will be kept.</li>
     *                             <li>{@code null}: Use the value of the property {@code ifState} is set. If not set,
     *                             the behaviour is the same as if it were {@code __tmp}</li>
     *                             </ul>
     * @return The IRI is returned if the member was deleted, otherwise null.
     */
    public static String explicitDelete(String iri, String stateDirPathStr) {

        if (iri == null || iri.contains(MAGIC_MARKER) || iri.contains(MAGIC_MARKER_ENCODED))
            return null;

        final String actualStateDirPathStr = IDLabFunctions.resolveStateDirPath(stateDirPathStr, "explicit_delete_state");

        /* Return IRI if the value is new, otherwise return NULL */
        if (EXPLICIT_DELETE_STATE.contains(actualStateDirPathStr, iri)) {
            return null;
        } else {
            EXPLICIT_DELETE_STATE.add(actualStateDirPathStr, iri);
            return iri;
        }
    }

    /**
     * Sorts the properties from a given resolved rr:template by property name. E.g.: if the template is
     * {@code b=1&a=3} the the result will be {@code a=3&b=1}
     * @param watchedValueTemplate  Property-value template to sort
     * @return  The same property-value template sorted according to property name.
     */
    static String sortWatchedProperties(final String watchedValueTemplate) {
        String[] propertyAndValues = watchedValueTemplate.split("&");   // TODO: in theory rr:templates are more complex than this...
        Arrays.sort(propertyAndValues);
        return String.join("&", propertyAndValues);
    }

    /**
     * Concatenates two strings, optionally separated by a delimiter.
     * @param str1  The first string
     * @param str2  The second string
     * @param delimiter This will be placed between the two strings in the result, or not if it is null.
     * @return      A String in the form {@code "<str1><delimiter><str2>"}
     */
    public static String concat(final String str1, final String str2, final String delimiter) {
        final String sep = delimiter == null? "" : delimiter;
        return str1 + sep + str2;
    }

    /**
     * Concatenates the elements of a list
     * @param seq       A list of CharSequences
     * @param delimiter An optional delimiter
     * @return          A new String that is composed of the seq arguments
     */
    public static String concatSequence(final List<CharSequence> seq, final String delimiter) {
        final String sep = delimiter == null? "" : delimiter;
        return String.join(sep, seq);
    }


    // TODO check whether this is the right place for this
    public static String jsonize(Object s) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(s);
    }


    public static String lookup(String searchString, String inputFile, Integer fromColumn, Integer toColumn) throws IOException, CsvValidationException {
        return lookupWithDelimiter(searchString, inputFile, fromColumn, toColumn, ",");
    }

    /**
     * It is a function that looks for the first occurrence of a certain value in a column of a csv file,
     * in order to return a value from a different column in the same row.
     *
     * @param searchString The string which needs to be found
     * @param inputFile    The path of a csv file in which the searchString needs to be found.
     * @param fromColumn   The index of the column which should contain the search string, numbering starts with 0
     * @param toColumn     The index of the column which should contain the return value, numbering starts with 0
     * @param delimiter    The delimiter used in the csv file, default is ','
     * @return String found in the toColumn on the first row containing the searchString in the fromColumn. If a column
     * index is out of range or if the searchString is not found, a message is logged and null is returned
     */
    public static String lookupWithDelimiter(String searchString, String inputFile, Integer fromColumn, Integer toColumn, String delimiter) throws IOException, CsvValidationException {

        //check if the LOOKUP_STATE_MAP contains the right values
           CSVReader reader = createReader(inputFile, delimiter);
           if(reader != null) {
               String[] nextLine = reader.readNext();
               if (fromColumn < 0 || toColumn < 0 || fromColumn >= nextLine.length || toColumn >= nextLine.length) {
                   logger.error("Column index out of boundaries; inputFile: \"{}\", fromColumn: \"{}\", toColumn: \"{}\"", inputFile, fromColumn, toColumn);
                   return null;
               }
               while (nextLine != null) {
                   // only save first occurrence in hashmap
                   if (!LOOKUP_STATE_MAP.containsKey(nextLine[fromColumn])) {
                       LOOKUP_STATE_MAP.put(nextLine[fromColumn], nextLine[toColumn]);
                   }
                   nextLine = reader.readNext();
               }
               reader.close();
           }

           String result = LOOKUP_STATE_MAP.get(searchString);

        if (result == null) {
            logger.error("The searchString is not found; searchString: \"{}\", inputFile: \"{}\", fromColumn: \"{}\"", searchString, inputFile, fromColumn);
        }
        return result;
    }

    /**
     * It is a function that looks for the first occurrence of certain values in the columns of a csv file,
     * in order to return a value from the needed column in the same row.
     *
     * @param searchValues The values to match row on.
     * @param fromColumns The columns that connect values to the columns of csv file.
     * @param inputFile The path of a csv file in which the searchValues needs to be found.
     * @param toColumn The index of column the value of witch should be found.
     * @param delimiter The delimiter used in the csv file.
     * @return List of String type that contains found row values from the given csv file. If there is no match null is returned.
     * @throws IOException
     * @throws CsvValidationException
     */


    public static String multipleLookup(List<String> searchValues, List<Integer> fromColumns, String inputFile, Integer toColumn, String delimiter) throws IOException, CsvValidationException {
        SearchParameters pair = new SearchParameters(searchValues, fromColumns, inputFile);

        if (MULTIPLE_LOOKUP_STATE_MAP.containsKey(pair)) {
            return MULTIPLE_LOOKUP_STATE_MAP.get(pair);
        }

        if(!CACHE.containsKey(inputFile) || CACHE.get(inputFile) == null) {
            CSVReader reader = createReader(inputFile, delimiter);
            return Cache.fileToCache(searchValues, fromColumns, inputFile, toColumn, reader);
        }else {
            return Cache.readFromCache(searchValues, fromColumns, inputFile, toColumn);
        }

    }


    public static String multipleLookup(String firstStr, String secondStr, String thirdStr, String fourthStr, String fifthStr, String sixthStr,
                                              Integer first, Integer second, Integer third, Integer fourth, Integer fifth, Integer sixth,
                                              String inputFile, Integer toColumn, String delimiter) throws IOException, CsvValidationException {
        List<String> values = new ArrayList<>(Arrays.asList(firstStr, secondStr, thirdStr, fourthStr, fifthStr, sixthStr))
                .stream().filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Integer> indexes = new ArrayList<>(Arrays.asList(first, second, third, fourth, fifth, sixth))
                .stream().filter(Objects::nonNull)
                .collect(Collectors.toList());
        return multipleLookup(values,indexes, inputFile,toColumn, delimiter);
    }


    private static CSVReader createReader(String inputFile, String delimiter) throws IOException {

        if(inputFile != null){
            InputStream inputStream = Files.newInputStream(new File(inputFile).toPath());
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator(delimiter.charAt(0)) //not passing a delimiter in fno results delimiter = null
                    .withIgnoreQuotations(true)
                    .build();
            return new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
                    .withCSVParser(parser)
                    .build();
        }
        return null;
    }

}
