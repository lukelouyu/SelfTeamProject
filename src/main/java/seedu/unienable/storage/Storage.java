package seedu.unienable.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.exception.StorageException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityOrder;

/**
 * Coordinates the individual *Storage classes against a shared data directory, so callers can load
 * or save everything through one entry point instead of wiring up each file path separately.
 */
public class Storage {
    private final Path activitiesFile;
    private final Path topicsFile;
    private final Path facilitiesFile;
    private final Path connectionsFile;
    private final Path settingsFile;

    private final ActivityStorage activityStorage = new ActivityStorage();
    private final TopicStorage topicStorage = new TopicStorage();
    private final FacilityStorage facilityStorage = new FacilityStorage();
    private final ConnectionStorage connectionStorage = new ConnectionStorage();
    private final SettingsStorage settingsStorage = new SettingsStorage();

    /**
     * Creates a Storage coordinator rooted at the given data directory.
     *
     * @param dataDirectory directory containing activities.txt, topics.txt, facilities.txt,
     *     connections.txt, and settings.txt
     */
    public Storage(Path dataDirectory) {
        this.activitiesFile = dataDirectory.resolve("activities.txt");
        this.topicsFile = dataDirectory.resolve("topics.txt");
        this.facilitiesFile = dataDirectory.resolve("facilities.txt");
        this.connectionsFile = dataDirectory.resolve("connections.txt");
        this.settingsFile = dataDirectory.resolve("settings.txt");
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
     * Loads activities from activities.txt, additionally rejecting any activity whose topic
     * field does not correspond to a topic in the given list (typically already loaded from
     * topics.txt). See {@link ActivityStorage#load(Path, List)}.
     *
     * @param validTopics every topic currently recorded in topics.txt
     * @return the loaded activities plus warnings for any skipped malformed or invalid lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Activity> loadActivities(List<Topic> validTopics) throws StorageException {
        return activityStorage.load(activitiesFile, validTopics);
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
     * Loads topics from topics.txt. Converts internally between the file format's
     * {@link TopicStorage.TopicRecord} pairs and the domain {@link Topic} class, so callers never
     * need to know about the storage-level record type.
     *
     * @return the loaded topics plus warnings for any skipped malformed lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Topic> loadTopics() throws StorageException {
        LoadResult<TopicStorage.TopicRecord> recordResult = topicStorage.load(topicsFile);
        List<Topic> topics = new ArrayList<>();
        for (TopicStorage.TopicRecord record : recordResult.getRecords()) {
            topics.add(new Topic(record.getCategory(), record.getName()));
        }
        return new LoadResult<>(topics, recordResult.getWarnings());
    }

    /**
     * Saves the given topics to topics.txt.
     *
     * @param topics the topics to save
     * @throws StorageException if a field contains the '|' delimiter, or the file cannot be written
     */
    public void saveTopics(List<Topic> topics) throws StorageException {
        List<TopicStorage.TopicRecord> records = new ArrayList<>();
        for (Topic topic : topics) {
            records.add(new TopicStorage.TopicRecord(topic.getCategory(), topic.getName()));
        }
        topicStorage.save(topicsFile, records);
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

    /**
     * Loads connections from connections.txt, additionally rejecting any connection whose "from"
     * or "to" endpoint does not match a known facility's name. See
     * {@link ConnectionStorage#load(Path, List)}.
     *
     * @param knownFacilities every facility currently recorded in facilities.txt
     * @return the loaded connections plus warnings for any skipped malformed or invalid lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Connection> loadConnections(List<Facility> knownFacilities) throws StorageException {
        return connectionStorage.load(connectionsFile, knownFacilities);
    }

    /**
     * Loads the saved default activity order from settings.txt.
     *
     * @return a LoadResult whose single record is the saved order (or the documented default,
     *     with a warning, if the file/line is missing or malformed)
     * @throws StorageException if the file exists but cannot be read
     */
    public LoadResult<ActivityOrder> loadSettings() throws StorageException {
        return settingsStorage.loadDefaultOrder(settingsFile);
    }

    /**
     * Saves the given default activity order to settings.txt.
     *
     * @param order the order to save
     * @throws StorageException if the file cannot be written
     */
    public void saveSettings(ActivityOrder order) throws StorageException {
        settingsStorage.saveDefaultOrder(settingsFile, order);
    }

    /**
     * Saves activities, topics, and the default activity order together, so a multi-file mutation
     * (e.g. "reset all", which touches all three) cannot leave activities.txt, topics.txt, and
     * settings.txt disagreeing with each other on disk. Each file is first written to a sibling
     * temporary file; only once every write has succeeded are the temporary files moved into
     * place. If any write fails, no original file is touched and the temporary files are removed,
     * so a partial failure leaves every file exactly as it was before this call.
     *
     * @param activities the activities to save
     * @param topics the topics to save
     * @param order the default activity order to save
     * @throws StorageException if a field contains the '|' delimiter, or any file cannot be
     *     written or committed
     */
    public void saveAll(List<Activity> activities, List<Topic> topics, ActivityOrder order)
            throws StorageException {
        Path activitiesTmp = tempSiblingOf(activitiesFile);
        Path topicsTmp = tempSiblingOf(topicsFile);
        Path settingsTmp = tempSiblingOf(settingsFile);
        try {
            activityStorage.save(activitiesTmp, activities);
            topicStorage.save(topicsTmp, toTopicRecords(topics));
            settingsStorage.saveDefaultOrder(settingsTmp, order);

            checkWritable(activitiesFile);
            checkWritable(topicsFile);
            checkWritable(settingsFile);

            commit(activitiesTmp, activitiesFile);
            commit(topicsTmp, topicsFile);
            commit(settingsTmp, settingsFile);
        } catch (StorageException e) {
            deleteQuietly(activitiesTmp);
            deleteQuietly(topicsTmp);
            deleteQuietly(settingsTmp);
            throw e;
        }
    }

    private List<TopicStorage.TopicRecord> toTopicRecords(List<Topic> topics) {
        List<TopicStorage.TopicRecord> records = new ArrayList<>();
        for (Topic topic : topics) {
            records.add(new TopicStorage.TopicRecord(topic.getCategory(), topic.getName()));
        }
        return records;
    }

    private Path tempSiblingOf(Path file) {
        return file.resolveSibling(file.getFileName() + ".tmp");
    }

    /**
     * Rejects committing to a destination that exists but is not writable, checked for every
     * destination before any of them is moved into place. Without this upfront check, a
     * permission failure discovered partway through the commit sequence (e.g. topics.txt is
     * read-only) would leave an earlier file (e.g. activities.txt) already overwritten while a
     * later one is not - exactly the memory/disk divergence this method exists to prevent.
     *
     * @param destination the file about to be committed to
     * @throws StorageException if destination exists and is not writable
     */
    private void checkWritable(Path destination) throws StorageException {
        if (Files.exists(destination) && !Files.isWritable(destination)) {
            throw new StorageException("could not write " + destination + " (not writable)");
        }
    }

    private void commit(Path tempFile, Path destination) throws StorageException {
        try {
            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("could not write " + destination, e);
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Best-effort cleanup only; the original files were never touched, so a leftover
            // temporary file does not put the application in an inconsistent state.
        }
    }

    /**
     * Copies the bundled default facilities.txt and connections.txt into the data directory, for
     * each file that does not already exist there. An existing file is left untouched, so manual
     * edits made between application runs are preserved.
     *
     * @throws StorageException if the data directory cannot be created, a bundled default is
     *     missing from the application, or a default file cannot be written
     */
    public void copyDefaultAccessibilityDataIfMissing() throws StorageException {
        copyResourceIfMissing("facilities.txt", facilitiesFile);
        copyResourceIfMissing("connections.txt", connectionsFile);
    }

    /**
     * Ensures every data file exists before the first load: creates empty activities.txt and
     * topics.txt if missing (there is no bundled default user data), and copies the bundled
     * default facilities.txt/connections.txt if missing. Existing files are always left untouched.
     *
     * @throws StorageException if the data directory or a file cannot be created
     */
    public void prepareDataFiles() throws StorageException {
        createEmptyFileIfMissing(activitiesFile);
        createEmptyFileIfMissing(topicsFile);
        copyDefaultAccessibilityDataIfMissing();
    }

    private void createEmptyFileIfMissing(Path file) throws StorageException {
        if (Files.exists(file)) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        } catch (IOException e) {
            throw new StorageException("could not create " + file, e);
        }
    }

    private void copyResourceIfMissing(String resourceName, Path destination) throws StorageException {
        if (Files.exists(destination)) {
            return;
        }
        try {
            Files.createDirectories(destination.getParent());
            try (InputStream resource = getClass().getResourceAsStream("/" + resourceName)) {
                if (resource == null) {
                    throw new StorageException("bundled default \"" + resourceName + "\" is missing "
                            + "from the application");
                }
                Files.copy(resource, destination);
            }
        } catch (IOException e) {
            throw new StorageException("could not create default " + destination, e);
        }
    }
}
