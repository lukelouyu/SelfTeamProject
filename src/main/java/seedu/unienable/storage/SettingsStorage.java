package seedu.unienable.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.enums.ActivityOrder;

/**
 * Loads and saves the persisted default activity order from/to a single-line settings.txt file.
 *
 * <p>Format: {@code ORDER|name}, e.g. {@code ORDER|CHRONOLOGICAL}. A missing file, or a missing,
 * unrecognised, or malformed ORDER line, falls back to the documented default (CHRONOLOGICAL)
 * with a warning rather than failing to start, the same way the other data files degrade.
 */
public class SettingsStorage {
    private static final String ORDER_TAG = "ORDER";
    private static final ActivityOrder DEFAULT_ORDER = ActivityOrder.CHRONOLOGICAL;

    /**
     * Loads the saved default activity order.
     *
     * @param filePath path to the settings text file
     * @return a LoadResult whose single record is the saved order (or the default, with a
     *     warning, if the file is missing or its ORDER line is absent/malformed)
     * @throws StorageException if the file exists but cannot be read
     */
    public LoadResult<ActivityOrder> loadDefaultOrder(Path filePath) throws StorageException {
        if (!Files.exists(filePath)) {
            return new LoadResult<>(List.of(DEFAULT_ORDER), List.of());
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new StorageException("could not read " + filePath, e);
        }

        List<String> warnings = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\\|", -1);
            if (fields.length != 2 || !ORDER_TAG.equals(fields[0])) {
                warnings.add("Line " + (i + 1) + " was skipped: unknown record type \"" + fields[0] + "\"");
                continue;
            }
            try {
                return new LoadResult<>(List.of(ActivityOrder.valueOf(fields[1])), warnings);
            } catch (IllegalArgumentException e) {
                warnings.add("Line " + (i + 1) + " was skipped: invalid order \"" + fields[1] + "\"");
            }
        }
        return new LoadResult<>(List.of(DEFAULT_ORDER), warnings);
    }

    /**
     * Saves the given default activity order, overwriting any existing content.
     *
     * @param filePath path to the settings text file
     * @param order the order to save
     * @throws StorageException if the file cannot be written
     */
    public void saveDefaultOrder(Path filePath, ActivityOrder order) throws StorageException {
        try {
            Files.write(filePath, List.of(ORDER_TAG + "|" + order.name()));
        } catch (IOException e) {
            throw new StorageException("could not write " + filePath, e);
        }
    }
}
