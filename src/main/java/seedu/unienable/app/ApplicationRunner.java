package seedu.unienable.app;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.StorageException;
import seedu.unienable.exception.UniEnableException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.parser.CommandDispatcher;
import seedu.unienable.storage.LoadResult;
import seedu.unienable.storage.Storage;
import seedu.unienable.storage.TopicStorage;
import seedu.unienable.ui.Ui;

/**
 * Coordinates one run of the application: configuring startup, loading and populating stored
 * data, running the read-execute-print command loop, and persisting state after every executed
 * command. Owns every lifecycle dependency (UI, input, storage, managers, dispatcher,
 * confirmation handler) as fields, so its methods stay focused on coordination rather than
 * threading dependencies through long parameter lists.
 */
public class ApplicationRunner {
    private final Path dataDirectory;
    private final InputStream input;

    private Ui ui;
    private Scanner scanner;
    private Storage storage;
    private ActivityManager activityManager;
    private TopicManager topicManager;
    private FacilityManager facilityManager;
    private ConnectionManager connectionManager;
    private CommandDispatcher dispatcher;
    private CommandConfirmationHandler confirmationHandler;

    /**
     * Creates an ApplicationRunner for one run of the application.
     *
     * @param dataDirectory the directory containing (or to create) the five data files
     * @param input the source of command-line input, e.g. System.in; never closed by this class
     */
    public ApplicationRunner(Path dataDirectory, InputStream input) {
        this.dataDirectory = dataDirectory;
        this.input = input;
    }

    /**
     * Runs the full application: shows the welcome message, loads saved data, then processes
     * commands until "bye" or the input stream ends. If startup data loading fails, shows the
     * error and returns without starting the command loop.
     */
    public void run() {
        suppressJdkLoggingFromLeakingIntoCliOutput();

        ui = new Ui();
        ui.showWelcome();

        scanner = new Scanner(input);
        storage = new Storage(dataDirectory);
        activityManager = new ActivityManager();
        topicManager = new TopicManager(activityManager);
        confirmationHandler = new CommandConfirmationHandler(ui, scanner, activityManager);

        if (!initialise()) {
            return;
        }

        dispatcher = new CommandDispatcher(activityManager, topicManager, facilityManager, connectionManager);
        runCommandLoop();
    }

    /**
     * Suppresses the JVM's default console log handler. Activity/Topic mutations log at INFO for
     * internal diagnostics; without this, those records would interleave with the framed CLI
     * output on stderr.
     */
    private void suppressJdkLoggingFromLeakingIntoCliOutput() {
        Logger.getLogger("").setLevel(Level.WARNING);
    }

    /**
     * Loads every data file, populates the in-memory managers, and shows any partial-load
     * warnings.
     *
     * @return true if loading succeeded and the command loop may start; false if a fatal storage
     *     failure occurred (already reported to the user)
     */
    private boolean initialise() {
        try {
            storage.prepareDataFiles();
            LoadResult<Activity> activityLoad = storage.loadActivities();
            LoadResult<TopicStorage.TopicRecord> topicLoad = storage.loadTopics();
            LoadResult<Facility> facilityLoad = storage.loadFacilities();
            LoadResult<Connection> connectionLoad = storage.loadConnections();
            LoadResult<ActivityOrder> settingsLoad = storage.loadSettings();

            activityManager.loadAll(activityLoad.getRecords());
            topicManager.loadAll(toTopics(topicLoad.getRecords()));
            facilityManager = new FacilityManager(facilityLoad.getRecords());
            connectionManager = new ConnectionManager(connectionLoad.getRecords());
            activityManager.setDefaultOrder(settingsLoad.getRecords().get(0));

            showLoadWarnings("activities.txt", activityLoad);
            showLoadWarnings("topics.txt", topicLoad);
            showLoadWarnings("facilities.txt", facilityLoad);
            showLoadWarnings("connections.txt", connectionLoad);
            showLoadWarnings("settings.txt", settingsLoad);
            return true;
        } catch (UniEnableException e) {
            showStartupError(e);
            return false;
        }
    }

    /**
     * Displays a fatal startup error, in the same framed shape as any other application error.
     *
     * @param exception the error that prevented startup from completing
     */
    private void showStartupError(UniEnableException exception) {
        ui.showFramed("[Error] " + exception.getErrorCategory() + ": " + exception.getMessage());
    }

    private void showLoadWarnings(String fileName, LoadResult<?> loadResult) {
        if (!loadResult.hasWarnings()) {
            return;
        }
        StringBuilder message = new StringBuilder("[Warning] Partial data loaded: ").append(fileName);
        for (String warning : loadResult.getWarnings()) {
            message.append('\n').append(warning);
        }
        ui.showFramed(message.toString());
    }

    /** Reads one line of input at a time until "bye" or the input stream ends. Blank lines are ignored. */
    private void runCommandLoop() {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isBlank()) {
                continue;
            }
            if (!processCommand(line)) {
                break;
            }
        }
    }

    /**
     * Dispatches, confirms if needed, and executes one line of input, then persists application
     * state if it mutated anything.
     *
     * @param line one full line of raw user input
     * @return true if the command loop should keep reading input; false if this command should
     *     end the application (e.g. "bye")
     */
    private boolean processCommand(String line) {
        try {
            Command command = dispatcher.dispatch(line, LocalDateTime.now());
            if (!confirmationHandler.confirmIfNeeded(command)) {
                return true;
            }
            CommandResult result = command.execute();
            ui.showFramed(result.getFeedback());
            saveApplicationState();
            return !result.isShouldExit();
        } catch (UniEnableException e) {
            ui.showFramed("[Error] " + e.getErrorCategory() + ": " + e.getMessage());
            return true;
        }
    }

    /**
     * Persists every piece of mutable application state: activities, topics, and the saved
     * default activity order.
     *
     * @throws StorageException if any file cannot be written
     */
    private void saveApplicationState() throws StorageException {
        storage.saveActivities(activityManager.getAll());
        storage.saveTopics(toRecords(topicManager));
        storage.saveSettings(activityManager.getDefaultOrder());
    }

    private static List<Topic> toTopics(List<TopicStorage.TopicRecord> records) {
        List<Topic> topics = new ArrayList<>();
        for (TopicStorage.TopicRecord record : records) {
            topics.add(new Topic(record.getCategory(), record.getName()));
        }
        return topics;
    }

    private static List<TopicStorage.TopicRecord> toRecords(TopicManager topicManager) {
        List<TopicStorage.TopicRecord> records = new ArrayList<>();
        for (ActivityCategory category : ActivityCategory.values()) {
            for (Topic topic : topicManager.list(category)) {
                records.add(new TopicStorage.TopicRecord(topic.getCategory(), topic.getName()));
            }
        }
        return records;
    }
}
