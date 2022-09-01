package be.ugent.knows.util;

import java.util.List;

public class SearchParameters {



    private final List<String> filters;
    private final List<Integer> columns;
    private final String fileName;
    private List<String> rowMatch = null;

    private String result = null;
    public SearchParameters(List<String> filters, List<Integer> columns, String name){

        if(name == null || filters == null || columns == null){
            throw new IllegalArgumentException(
                    String.format("Search string, columns and input file should not be null, was given:inputFile: \"%s\", searchValues: \"%s\", columns: \"%s\"", name, filters, columns)
            );
        }

        this.fileName= name;
        this.filters = filters;
        this.columns = columns;
    }

    @Override
    public int hashCode() {
        return this.filters.hashCode() * this.fileName.hashCode() * this.columns.hashCode();
    }

    @Override
    public boolean equals(Object o){
        SearchParameters pair = (SearchParameters) o;
        return this.filters.equals(pair.filters)
                && this.fileName.equals(pair.fileName)
                && this.columns.equals(pair.columns);
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
