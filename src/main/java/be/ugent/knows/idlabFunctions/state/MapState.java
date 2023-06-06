package be.ugent.knows.idlabFunctions.state;

import java.util.Optional;

/**
 * A MapState is a map of states, where the key is a path to a file where
 * the state is persisted and the value is a state. A state is again a
 * map of key - value Strings.
 *
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
     * @param stateFilePath The path of this state map's persistence file.
     */
    String put(final String stateFilePath, final String key, final String value);

    /**
     * Loads or creates a new state map for the given state file path.
     * Adds the specified value to the associated values of the specified key in this map,
     * (optional operation). If the value is not associated with the key, the index (i.e.,
     * the number of values - 1) is returned. If the value is already associated, an empty
     * value is returned.
     * @param stateFilePath The path of this state map's persistence file.
     * @param key           The key with which the specified value is to be associated.
     * @param value         The value to be associated with the specified key.
     * @return              If {@code value} was not previously associated with {@code key}: {@code Optional.empty()};
     *                      else an Optional with the number of elements - 1 as value (i.e., the index)
     */
    Optional<Integer> putAndReturnIndex(final String stateFilePath, final String key, final String value);

    /**
     * Checks if an key exists or not.
     * @param stateFilePath The path of this state map's persistence file.
     * @param key           The key with which the specified value is to be associated.
     * @return              True if the pair exists, otherwise false.
     */
    boolean hasKey(String stateFilePath, String key);

    /**
     * Deletes all state: removes state files and clears the state in memory.
     */
    void deleteAllState();

    /**
     * Writes all state to disk, keeping all data in memory.
     */
    void saveAllState();

    /**
     * Counts the number of entries in the state for a given state file path.
     * @param stateFilePath The path of this state map's persistence file.
     * @param key           The key to count the values for.
     * @return              The number of entries in the state.
     */
    long count(final String stateFilePath, final String key);
}
