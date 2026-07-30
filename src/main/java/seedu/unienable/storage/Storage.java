package seedu.unienable.storage;

import java.nio.file.Path;
import java.util.List;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.exception.StorageException;
import seedu.unienable.model.classes.Activity;

/**
 * Coordinates the individual *Storage classes against a shared data directory, so callers can load
 * or save everything through one entry point instead of wiring up each file path separately.
 */
public class Storage {
    private final Path activitiesFile;
    private final Path topicsFile;
    private final Path facilitiesFile;
    private final Path connectionsFile;

    private final ActivityStorage activityStorage = new ActivityStorage();
    private final TopicStorage topicStorage = new TopicStorage();
    private final FacilityStorage facilityStorage = new FacilityStorage();
    private final ConnectionStorage connectionStorage = new ConnectionStorage();

    /**
     * Creates a Storage coordinator rooted at the given data directory.
     *
     * @param dataDirectory directory containing activities.txt, topics.txt, facilities.txt, and
     *     connections.txt
     */
    public Storage(Path dataDirectory) {
        this.activitiesFile = dataDirectory.resolve("activities.txt");
        this.topicsFile = dataDirectory.resolve("topics.txt");
        this.facilitiesFile = dataDirectory.resolve("facilities.txt");
        this.connectionsFile = dataDirectory.resolve("connections.txt");
    }

    /**
     * Loads activities from activities.txt.
     *
     * @return the loaded activities plus warnings for any skipped malformed lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Activity> loadActivities() throws StorageException {
        return activityStorage.load(activitiesFile);
    }

    /**
     * Saves the given activities to activities.txt.
     *
     * @param activities the activities to save
     * @throws StorageException if a field contains the '|' delimiter, or the file cannot be written
     */
    public void saveActivities(List<Activity> activities) throws StorageException {
        activityStorage.save(activitiesFile, activities);
    }

    /**
     * Loads topic records from topics.txt.
     *
     * @return the loaded topic records plus warnings for any skipped malformed lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<TopicStorage.TopicRecord> loadTopics() throws StorageException {
        return topicStorage.load(topicsFile);
    }

    /**
     * Saves the given topic records to topics.txt.
     *
     * @param topics the topic records to save
     * @throws StorageException if a field contains the '|' delimiter, or the file cannot be written
     */
    public void saveTopics(List<TopicStorage.TopicRecord> topics) throws StorageException {
        topicStorage.save(topicsFile, topics);
    }

    /**
     * Loads facilities from facilities.txt. Read-only: no in-app command adds, edits, or deletes
     * facility records.
     *
     * @return the loaded facilities plus warnings for any skipped malformed lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Facility> loadFacilities() throws StorageException {
        return facilityStorage.load(facilitiesFile);
    }

    /**
     * Loads connections from connections.txt. Read-only: no in-app command adds, edits, or deletes
     * connection records.
     *
     * @return the loaded connections plus warnings for any skipped malformed lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Connection> loadConnections() throws StorageException {
        return connectionStorage.load(connectionsFile);
    }
}
