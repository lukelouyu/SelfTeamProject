package seedu.unienable.storage;

import java.util.Collections;
import java.util.List;

/** The result of loading records from a storage file: the records that parsed, plus warnings for skipped lines. */
public class LoadResult<T> {
    private final List<T> records;
    private final List<String> warnings;

    /**
     * Creates a LoadResult.
     *
     * @param records the records that parsed successfully
     * @param warnings one message per skipped malformed line, if any
     */
    public LoadResult(List<T> records, List<String> warnings) {
        this.records = Collections.unmodifiableList(records);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    /** Returns the records that parsed successfully, as an unmodifiable list. */
    public List<T> getRecords() {
        return records;
    }

    /** Returns one message per skipped malformed line, as an unmodifiable list. */
    public List<String> getWarnings() {
        return warnings;
    }

    /** Returns whether any line was skipped during loading. */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
