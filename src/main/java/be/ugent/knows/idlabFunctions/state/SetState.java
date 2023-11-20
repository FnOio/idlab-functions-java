package be.ugent.knows.idlabFunctions.state;

public interface SetState<T> extends AutoCloseable {
    /**
     * Checks if an value exists or not.
     *
     * @param stateFilePath The path of this state map's persistence file.
     * @param value           The value with which the specified value is to be associated.
     * @return True if the pair exists, otherwise false.
     **/
    boolean contains(final String stateFilePath, final T value);

    /**
     * Writes all state to disk, keeping all data in memory.
     */
    void saveAllState();

    /**
     * Deletes all state: removes state files and clears the state in memory.
     */
    void deleteAllState();

    /**
     * Loads or creates a new state set for the given state file path.
     * Adds the specified element to this set if not present.
     * <br>
     * Note that the state is only persisted to file upon {@link #close()}.
     *
     * @param value value to be associated with the specified key
     * @param stateFilePath The path of this state map's persistence file.
     */
    void add(final String stateFilePath, final T value);
}
