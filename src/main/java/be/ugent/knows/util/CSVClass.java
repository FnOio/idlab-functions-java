package be.ugent.knows.util;

import java.util.*;
import java.util.stream.Collectors;

public class CSVClass {

    private String fileName;
    private final List<List<String>> data;
    private List<String> result;

    public CSVClass(){
        this.fileName = null;
        this.data = new ArrayList<>();
        this.result = null;
    }

    public List<String> getResult() {
        return result;
    }

    public String getFileName() {
        return fileName;
    }

    public void setName(String name) {
        this.fileName = name;
    }

    public void addValue(List<String> line){
        this.data.add(line);
    }

    public List<String> find(List<String> values){
        if (values == null || values.isEmpty()){
            return null;
        }
        for (List<String> s: this.data) {
            if(new HashSet<>(s).containsAll(values)){
                ArrayList<String> temp = new ArrayList<>(s);
                temp.removeAll(values);
                this.result = temp;
                return this.result;
            }
        }
        return null;
    }



}
