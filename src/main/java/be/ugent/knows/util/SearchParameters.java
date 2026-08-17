package be.ugent.knows.util;

import java.util.List;
import java.util.Objects;

public class SearchParameters {



    private final List<String> filters;
    private final List<Integer> columns;
    private final String fileName;
    private final Integer toColumn;
    private final String delimiter;
    private List<String> rowMatch = null;

    private String result = null;
    public SearchParameters(List<String> filters, List<Integer> columns, String name){
        this(filters, columns, name, null, null);
    }

    public SearchParameters(List<String> filters, List<Integer> columns, String name, Integer toColumn, String delimiter){

        if(name == null || filters == null || columns == null){
            throw new IllegalArgumentException(
                    String.format("Search string, columns and input file should not be null, was given:inputFile: \"%s\", searchValues: \"%s\", columns: \"%s\"", name, filters, columns)
            );
        }

        this.fileName= name;
        this.filters = filters;
        this.columns = columns;
        this.toColumn = toColumn;
        this.delimiter = delimiter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.filters, this.fileName, this.columns, this.toColumn, this.delimiter);
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof SearchParameters pair)) {
            return false;
        }
        return this.filters.equals(pair.filters)
                && this.fileName.equals(pair.fileName)
            && this.columns.equals(pair.columns)
            && Objects.equals(this.toColumn, pair.toColumn)
            && Objects.equals(this.delimiter, pair.delimiter);
    }

    @Override
    public String toString(){
        return "\n" + "filters: " + this.filters.toString() +
                " columns: " + this.columns.toString() +
                " file: " + this.fileName;
    }
    public List<String> getFilters() {
        return filters;
    }

    public String getFileName() {
        return fileName;
    }
    public List<String> getRowMatch(){
        return this.rowMatch;
    }
    public void setRowMatch(List<String> rowMatch) {
        this.rowMatch = rowMatch;
    }
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
