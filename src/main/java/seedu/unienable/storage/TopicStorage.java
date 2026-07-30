package seedu.unienable.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.enums.ActivityCategory;

/**
 * Loads and saves topic records from/to a pipe-delimited topics.txt-format file.
 *
 * <p>Format: {@code TOPIC|category|name}. A dedicated Topic model class is deferred to
 * v1-topic-category; until then this stores plain category/name pairs. Fields must not contain
 * the '|' delimiter; this is not escaped in v1.0.
 */
public class TopicStorage {
    private static final String TOPIC_TAG = "TOPIC";
    private static final String DELIMITER = "|";

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

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                topics.add(parseLine(line));
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
        if (fields.length < 3) {
            throw new IllegalArgumentException("TOPIC line requires a category and a name");
        }
        ActivityCategory category;
        try {
            category = ActivityCategory.valueOf(fields[1]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid category \"" + fields[1] + "\"");
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

    /** A category/name pair loaded from or saved to topics.txt, ahead of a dedicated Topic model class. */
    public static final class TopicRecord {
        private final ActivityCategory category;
        private final String name;

        public TopicRecord(ActivityCategory category, String name) {
            this.category = category;
            this.name = name;
        }

        public ActivityCategory getCategory() {
            return category;
        }

        public String getName() {
            return name;
        }
    }
}
