package be.ugent.knows.idlabFunctions.state;

/**
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public interface MapState extends AutoCloseable {
    String put(final String stateFilePath, final String key, final String value);
    void deleteAllState();
}
