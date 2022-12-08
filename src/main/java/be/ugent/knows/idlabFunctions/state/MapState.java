package be.ugent.knows.idlabFunctions.state;

/**
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public interface MapState extends AutoCloseable {

    /**
     * Loads or creates a new state map for the given state file path.
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <br>
     * Note that the state is only persisted to file upon {@link #close()}.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key},
     *         if the implementation supports {@code null} values.)
     * @param stateFilePath The path of this state map's persistance file.
     */
    String put(final String stateFilePath, final String key, final String value);

    /**
     * Deletes all state: removes state files and clears the state in memory.
     */
    void deleteAllState();

    /**
     * Writes all state to disk, keeping all data in memory.
     */
    void saveAllState();
}
