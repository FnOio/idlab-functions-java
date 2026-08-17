package be.ugent.knows.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class Cache {

    private static final Map<String, List<String[]>> CACHE = new HashMap<>();
    public static List<String[]> getRows(String inputFile, String delimiter, CSVReader reader) throws IOException, CsvValidationException {
        String cacheKey = cacheKey(inputFile, delimiter);
        List<String[]> rows = CACHE.get(cacheKey);
        if (rows == null) {
            rows = new ArrayList<>();
            if (reader != null) {
                String[] nextLine;
                while ((nextLine = reader.readNext()) != null) {
                    rows.add(nextLine);
                }
                reader.close();
            }
            CACHE.put(cacheKey, rows);
        }
        return rows;
    }

    private static String cacheKey(String inputFile, String delimiter) {
        return inputFile + "\u0000" + delimiter;
    }

}
