package seedu.unienable.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.enums.ActivityCategory;

/**
 * Loads and saves topic records from/to a pipe-delimited topics.txt-format file.
 *
 * <p>Format: {@code TOPIC|category|name} - exactly three fields, no optional trailing ones.
 * Uses its own {@link TopicRecord} category/name pair rather than the
 * {@link seedu.unienable.model.classes.Topic} model class directly; the main loop converts
 * between them at load/save time. Fields must not contain the '|' delimiter; this is not escaped
 * in v1.0.
 *
 * <p>Loading rejects a blank name, an extra or missing field, and a duplicate category+name
 * (case-insensitive) - the same shapes topic creation through the app already rejects, so a
 * hand-edited file cannot load a state the app itself would never allow a user to create.
 */
public class TopicStorage {
    private static final String TOPIC_TAG = "TOPIC";
    private static final String DELIMITER = "|";
    private static final int EXPECTED_FIELD_COUNT = 3;

    /**
     * Loads topic records from the given file.
     *
     * @param filePath path to the topics text file
     * @return the loaded topic records plus warnings for any skipped malformed lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<TopicRecord> load(Path filePath) throws StorageException {
        List<String> lines = readLines(filePath);
        List<TopicRecord> topics = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                TopicRecord topic = parseLine(line);
                String key = topic.getCategory() + "|" + topic.getName().toLowerCase(Locale.ROOT);
                if (!seenKeys.add(key)) {
                    throw new IllegalArgumentException(
                            "duplicate topic \"" + topic.getName() + "\" under " + topic.getCategory());
                }
                topics.add(topic);
            } catch (IllegalArgumentException e) {
                warnings.add("Line " + (i + 1) + " was skipped: " + e.getMessage());
            }
        }
        return new LoadResult<>(topics, warnings);
    }

    /**
     * Saves the given topic records to the given file, overwriting any existing content.
     *
     * @param filePath path to the topics text file
     * @param topics the topic records to save
     * @throws StorageException if a field contains the '|' delimiter, or the file cannot be written
     */
    public void save(Path filePath, List<TopicRecord> topics) throws StorageException {
        List<String> lines = new ArrayList<>();
        for (TopicRecord topic : topics) {
            lines.add(toLine(topic));
        }
        try {
            Files.write(filePath, lines);
        } catch (IOException e) {
            throw new StorageException("could not write " + filePath, e);
        }
    }

    private TopicRecord parseLine(String line) {
        String[] fields = line.split("\\|", -1);
        if (!fields[0].equals(TOPIC_TAG)) {
            throw new IllegalArgumentException("unknown record type \"" + fields[0] + "\"");
        }
        if (fields.length != EXPECTED_FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "TOPIC line requires exactly a category and a name (found " + (fields.length - 1) + " fields)");
        }
        ActivityCategory category;
        try {
            category = ActivityCategory.valueOf(fields[1]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid category \"" + fields[1] + "\"");
        }
        if (fields[2].isBlank()) {
            throw new IllegalArgumentException("topic name must not be blank");
        }
        return new TopicRecord(category, fields[2]);
    }

    private String toLine(TopicRecord topic) throws StorageException {
        if (topic.getName().contains(DELIMITER)) {
            throw new StorageException("field \"" + topic.getName() + "\" must not contain '" + DELIMITER + "'");
        }
        return String.join(DELIMITER, TOPIC_TAG, topic.getCategory().name(), topic.getName());
    }

    private List<String> readLines(Path filePath) throws StorageException {
        try {
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new StorageException("could not read " + filePath, e);
        }
    }

    /** A category/name pair loaded from or saved to topics.txt. */
    public static final class TopicRecord {
        private final ActivityCategory category;
        private final String name;

        /**
         * Creates a TopicRecord.
         *
         * @param category the fixed category the topic belongs to
         * @param name the topic name
         */
        public TopicRecord(ActivityCategory category, String name) {
            this.category = category;
            this.name = name;
        }

        /** Returns the fixed category the topic belongs to. */
        public ActivityCategory getCategory() {
            return category;
        }

        /** Returns the topic name. */
        public String getName() {
            return name;
        }
    }
}
