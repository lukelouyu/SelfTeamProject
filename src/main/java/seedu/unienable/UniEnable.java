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
import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.DeleteCommand;
import seedu.unienable.command.EditCommand;
import seedu.unienable.command.TopicDeleteCommand;
import seedu.unienable.command.TopicRenameCommand;
import seedu.unienable.exception.UniEnableException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.parser.CommandDispatcher;
import seedu.unienable.storage.LoadResult;
import seedu.unienable.storage.Storage;
import seedu.unienable.storage.TopicStorage;
import seedu.unienable.ui.MessageFormatter;
import seedu.unienable.ui.Ui;

/** Main entry-point for the UniEnable application: loads saved data, then runs the command loop. */
public class UniEnable {
    private static final Path DATA_DIRECTORY = Paths.get("data");

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

            activityManager.loadAll(activityLoad.getRecords());
            topicManager.loadAll(toTopics(topicLoad.getRecords()));
            facilityManager = new FacilityManager(facilityLoad.getRecords());
            connectionManager = new ConnectionManager(connectionLoad.getRecords());

            showLoadWarnings(ui, "activities.txt", activityLoad);
            showLoadWarnings(ui, "topics.txt", topicLoad);
            showLoadWarnings(ui, "facilities.txt", facilityLoad);
            showLoadWarnings(ui, "connections.txt", connectionLoad);
        } catch (UniEnableException e) {
            ui.showFramed("[Error] " + e.getErrorCategory() + ": " + e.getMessage());
            return;
        }

        CommandDispatcher dispatcher = new CommandDispatcher(activityManager, topicManager, facilityManager,
                connectionManager);
        runLoop(dispatcher, new Scanner(input), ui, storage, activityManager, topicManager);
    }

    private static void runLoop(CommandDispatcher dispatcher, Scanner scanner, Ui ui, Storage storage,
            ActivityManager activityManager, TopicManager topicManager) {
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();
            if (input.isBlank()) {
                continue;
            }
            if (!runOneCommand(input, dispatcher, scanner, ui, storage, activityManager, topicManager)) {
                break;
            }
        }
    }

    private static boolean runOneCommand(String input, CommandDispatcher dispatcher, Scanner scanner, Ui ui,
            Storage storage, ActivityManager activityManager, TopicManager topicManager) {
        try {
            Command command = dispatcher.dispatch(input, LocalDateTime.now());
            if (!confirmIfNeeded(command, scanner, ui, activityManager)) {
                return true;
            }
            CommandResult result = command.execute();
            ui.showFramed(result.getFeedback());
            storage.saveActivities(activityManager.getAll());
            storage.saveTopics(toRecords(topicManager));
            return !result.isShouldExit();
        } catch (UniEnableException e) {
            ui.showFramed("[Error] " + e.getErrorCategory() + ": " + e.getMessage());
            return true;
        }
    }

    private static boolean confirmIfNeeded(Command command, Scanner scanner, Ui ui, ActivityManager activityManager)
            throws UniEnableException {
        if (command instanceof DeleteCommand) {
            DeleteCommand delete = (DeleteCommand) command;
            Activity activity = activityManager.getById(delete.getId());
            return confirm(scanner, ui, "You selected activity [" + delete.getId() + "]:\n"
                    + MessageFormatter.formatConcise(activity) + "\n\nDelete this activity? (y/n)");
        }
        if (command instanceof EditCommand) {
            EditCommand edit = (EditCommand) command;
            Activity oldActivity = activityManager.getById(edit.getId());
            String diff = MessageFormatter.formatChanges(oldActivity, edit.getNewActivity());
            if (diff.isEmpty()) {
                ui.showFramed("No changes to activity [" + edit.getId() + "].");
                return false;
            }
            return confirm(scanner, ui, diff + "\nSave changes? (y/n)");
        }
        if (command instanceof TopicRenameCommand) {
            TopicRenameCommand rename = (TopicRenameCommand) command;
            String diff = "Before: topic = " + rename.getOldName() + "\nAfter : topic = " + rename.getNewName();
            return confirm(scanner, ui, diff + "\nSave changes? (y/n)");
        }
        if (command instanceof TopicDeleteCommand) {
            TopicDeleteCommand delete = (TopicDeleteCommand) command;
            return confirm(scanner, ui, "Delete topic \"" + delete.getName() + "\" under "
                    + delete.getCategory() + "? (y/n)");
        }
        return true;
    }

    private static boolean confirm(Scanner scanner, Ui ui, String prompt) {
        ui.showFramed(prompt);
        String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "n";
        if ("y".equalsIgnoreCase(answer)) {
            return true;
        }
        ui.showFramed("Cancelled. No changes were made.");
        return false;
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
