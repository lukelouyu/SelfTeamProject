package seedu.unienable.storage;

import java.util.Collections;
import java.util.List;

/** The result of loading records from a storage file: the records that parsed, plus warnings for skipped lines. */
public class LoadResult<T> {
    private final List<T> records;
    private final List<String> warnings;

    public LoadResult(List<T> records, List<String> warnings) {
        this.records = Collections.unmodifiableList(records);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    public List<T> getRecords() {
        return records;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
