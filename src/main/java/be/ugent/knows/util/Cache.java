package be.ugent.knows.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Cache {

    private static final Map<String, List<String[]>> CACHE = new HashMap<>();
    private static final Map<SearchParameters, String> MULTIPLE_LOOKUP_STATE_MAP = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(Cache.class);
    public static Map<SearchParameters, String> getMultipleLookupStateMap(){
        return MULTIPLE_LOOKUP_STATE_MAP;
    }
    public static Map<SearchParameters, String> getCache(){
        return MULTIPLE_LOOKUP_STATE_MAP;
    }

    /**
     * Function that saves file to cache and returns the found value.
     * @param searchValues The values to match row on.
     * @param fromColumns The columns that connect values to the columns of csv file.
     * @param inputFile The path of a csv file in which the searchValues needs to be found.
     * @param toColumn The index of column the value of witch should be found.
     * @param reader The reader for the csv file.
     * @return found matching on filters value from the csv file.
     * @throws IOException
     * @throws CsvValidationException
     */

    public static String fileToCache(List<String> searchValues, List<Integer> fromColumns, String inputFile, Integer toColumn, CSVReader reader) throws IOException, CsvValidationException {
        SearchParameters pair = new SearchParameters(searchValues, fromColumns, inputFile);

        if (reader != null) {
            String[] nextLine = reader.readNext();

            if (areParametersIncorrect(nextLine, toColumn, searchValues, fromColumns, inputFile))
                return null;

            while (nextLine != null) {
                // save to cache
                rowToCache(inputFile, nextLine);

                if(pair.getRowMatch() == null) {
                    pair.setRowMatch(check(fromColumns, searchValues, nextLine));
                }

                createExtraSearchParameters(searchValues, fromColumns, inputFile, toColumn, Arrays.asList(nextLine));
                nextLine = reader.readNext();

            }

            reader.close();
            if (pair.getRowMatch() != null) {
                MULTIPLE_LOOKUP_STATE_MAP.put(pair, pair.getRowMatch().get(toColumn));
            }else {
                logger.error("The searchString is not found; searchString: \"{}\", inputFile: \"{}\", fromColumns: \"{}\"", searchValues, inputFile, fromColumns);
                return null;
            }
        }

        return MULTIPLE_LOOKUP_STATE_MAP.get(pair);
    }

    /**
     * Function that reads from cache and returns the found value.
     * @param searchValues The values to match row on.
     * @param fromColumns The columns that connect values to the columns of csv file.
     * @param inputFile The path of a csv file in which the searchValues needs to be found.
     * @param toColumn The index of column the value of witch should be found.
     * @return found matching on filters value from the csv file.
     */
    public static String readFromCache(List<String> searchValues, List<Integer> fromColumns, String inputFile, Integer toColumn){
        SearchParameters pair = new SearchParameters(searchValues, fromColumns, inputFile);
        String[] nextLine = CACHE.get(inputFile).get(0);

        if (areParametersIncorrect(nextLine, toColumn, searchValues, fromColumns, inputFile))
            return null;

        List<String[]> table= CACHE.get(inputFile);
        for (String[] strings : table) {
            if (pair.getRowMatch() == null)
                pair.setRowMatch(check(fromColumns, searchValues, strings));

            createExtraSearchParameters(searchValues, fromColumns, inputFile, toColumn, Arrays.asList(strings));
        }


        if (pair.getRowMatch() != null) {
            MULTIPLE_LOOKUP_STATE_MAP.put(pair, pair.getRowMatch().get(toColumn));
        }else {
            logger.error("The searchString is not found; searchString: \"{}\", inputFile: \"{}\", fromColumns: \"{}\"", searchValues, inputFile, fromColumns);
            return null;
        }

        return MULTIPLE_LOOKUP_STATE_MAP.get(pair);

    }

    /**
     * Function that creates SearchPairs based on the same parameters as original request.
     * @param searchValues The values to match row on from original request.
     * @param fromColumns The columns that connect values to the columns of csv file from original request.
     * @param inputFile The path of a csv file.
     * @param toColumn The index of column the value of witch should be found from original request.
     * @param line List of String containing the values of the current row.
     */


    private static void createExtraSearchParameters(List<String> searchValues, List<Integer> fromColumns, String inputFile, Integer toColumn, List<String> line) {
        List<String> values = fromColumns.stream().map(line::get).collect(Collectors.toList());
        SearchParameters extraPair = new SearchParameters(values, fromColumns, inputFile);
        extraPair.setRowMatch(line);

        if (!values.equals(searchValues) && !MULTIPLE_LOOKUP_STATE_MAP.containsKey(extraPair)) {
            MULTIPLE_LOOKUP_STATE_MAP.put(extraPair, extraPair.getRowMatch().get(toColumn));
        }
    }

    /**
     * Function that checks if all parameters that were passed are correct.
     * @param nextLine The values(row) that were next from cache or file.
     * @param searchValues The values to match row on from original request.
     * @param fromColumns The columns that connect values to the columns of csv file from original request.
     * @param inputFile The path of a csv file.
     * @param toColumn The index of column the value of witch should be found from original request.
     * @return Boolean value, true if all values are incorrect, false if they are correct.
     */

    private static boolean areParametersIncorrect(String[] nextLine, Integer toColumn, List<String> searchValues, List<Integer> fromColumns, String inputFile) {

        if (toColumn == null || searchValues.isEmpty() || toColumn < 0
                || toColumn >= nextLine.length || searchValues.size() != fromColumns.size()) {
            logger.error("Column index out of boundaries; inputFile: \"{}\", fromColumns: \"{}\", toColumn: \"{}\"", inputFile, fromColumns, toColumn);
            return true;
        }

        for (Integer index : fromColumns) {
            if (index < 0 || index > nextLine.length) {
                logger.error("Column index out of boundaries; inputFile: \"{}\", fromColumns: \"{}\", toColumn: \"{}\"", inputFile, fromColumns, toColumn);
                return true;
            }
        }
        return false;
    }

    private static List<String> check(List<Integer> fromColumns, List<String> searchValues, String[] nextLine){
        for (int i = 0; i < fromColumns.size(); i++) {
            if(!nextLine[fromColumns.get(i)].equals(searchValues.get(i))){
                return null;
            }
        }
        return Arrays.asList(nextLine);
    }

    /**
     * Function that helps to save values to cache
     * @param inputFile inputFile The path of a csv file.
     * @param row The values that need to be cached.
     */
    public static void rowToCache(String inputFile, String[] row){
        if (!CACHE.containsKey(inputFile)){
            CACHE.put(inputFile, new ArrayList<>());
        }
        CACHE.get(inputFile).add(row);
    }

}
