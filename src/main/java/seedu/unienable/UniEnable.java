package seedu.unienable;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.app.CommandConfirmationHandler;
import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
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

/** Main entry-point for the UniEnable application: loads saved data, then runs the command loop. */
public class UniEnable {
    private static final Path DATA_DIRECTORY = Paths.get("data");

    /**
     * Application entry point. Runs against the real "data" folder and standard input.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        run(DATA_DIRECTORY, System.in);
    }

    /**
     * Runs the full application: loads saved data from the given directory, then processes
     * commands from the given input stream until "bye" or the stream ends. Separated from
     * main() so tests can point it at a temporary directory instead of the real "data" folder.
     *
     * @param dataDirectory the directory containing (or to create) the four data files
     * @param input the source of command-line input, e.g. System.in
     */
    public static void run(Path dataDirectory, InputStream input) {
        // Activity/Topic mutations log at INFO for internal diagnostics; suppress the JVM's
        // default console handler so those records don't interleave with the framed CLI output.
        Logger.getLogger("").setLevel(Level.WARNING);

        Ui ui = new Ui();
        ui.showWelcome();

        Storage storage = new Storage(dataDirectory);
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        FacilityManager facilityManager;
        ConnectionManager connectionManager;

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

            showLoadWarnings(ui, "activities.txt", activityLoad);
            showLoadWarnings(ui, "topics.txt", topicLoad);
            showLoadWarnings(ui, "facilities.txt", facilityLoad);
            showLoadWarnings(ui, "connections.txt", connectionLoad);
            showLoadWarnings(ui, "settings.txt", settingsLoad);
        } catch (UniEnableException e) {
            ui.showFramed("[Error] " + e.getErrorCategory() + ": " + e.getMessage());
            return;
        }

        CommandDispatcher dispatcher = new CommandDispatcher(activityManager, topicManager, facilityManager,
                connectionManager);
        Scanner scanner = new Scanner(input);
        CommandConfirmationHandler confirmationHandler = new CommandConfirmationHandler(ui, scanner, activityManager);
        runLoop(dispatcher, scanner, ui, storage, activityManager, topicManager, confirmationHandler);
    }

    private static void runLoop(CommandDispatcher dispatcher, Scanner scanner, Ui ui, Storage storage,
            ActivityManager activityManager, TopicManager topicManager,
            CommandConfirmationHandler confirmationHandler) {
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();
            if (input.isBlank()) {
                continue;
            }
            if (!runOneCommand(input, dispatcher, ui, storage, activityManager, topicManager, confirmationHandler)) {
                break;
            }
        }
    }

    private static boolean runOneCommand(String input, CommandDispatcher dispatcher, Ui ui, Storage storage,
            ActivityManager activityManager, TopicManager topicManager,
            CommandConfirmationHandler confirmationHandler) {
        try {
            Command command = dispatcher.dispatch(input, LocalDateTime.now());
            if (!confirmationHandler.confirmIfNeeded(command)) {
                return true;
            }
            CommandResult result = command.execute();
            ui.showFramed(result.getFeedback());
            storage.saveActivities(activityManager.getAll());
            storage.saveTopics(toRecords(topicManager));
            storage.saveSettings(activityManager.getDefaultOrder());
            return !result.isShouldExit();
        } catch (UniEnableException e) {
            ui.showFramed("[Error] " + e.getErrorCategory() + ": " + e.getMessage());
            return true;
        }
    }

    private static void showLoadWarnings(Ui ui, String fileName, LoadResult<?> loadResult) {
        if (!loadResult.hasWarnings()) {
            return;
        }
        StringBuilder message = new StringBuilder("[Warning] Partial data loaded: ").append(fileName);
        for (String warning : loadResult.getWarnings()) {
            message.append('\n').append(warning);
        }
        ui.showFramed(message.toString());
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
