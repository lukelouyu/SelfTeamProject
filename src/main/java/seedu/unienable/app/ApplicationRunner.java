package seedu.unienable.app;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.activity.AddCommand;
import seedu.unienable.command.activity.DeleteCommand;
import seedu.unienable.command.activity.EditCommand;
import seedu.unienable.command.activity.MarkCommand;
import seedu.unienable.command.activity.OrderSetCommand;
import seedu.unienable.command.activity.UnmarkCommand;
import seedu.unienable.command.general.ResetCommand;
import seedu.unienable.command.recur.RecurCommand;
import seedu.unienable.command.topic.TopicAddCommand;
import seedu.unienable.command.topic.TopicDeleteCommand;
import seedu.unienable.command.topic.TopicRenameCommand;
import seedu.unienable.exception.StorageException;
import seedu.unienable.exception.UniEnableException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.parser.CommandDispatcher;
import seedu.unienable.storage.LoadResult;
import seedu.unienable.storage.Storage;
import seedu.unienable.ui.Ui;

/**
 * Coordinates one run of the application: configuring startup, loading and populating stored
 * data, running the read-execute-print command loop, and persisting state after every executed
 * command. Owns every lifecycle dependency (UI, input, storage, managers, dispatcher,
 * confirmation handler) as fields, so its methods stay focused on coordination rather than
 * threading dependencies through long parameter lists.
 */
public class ApplicationRunner {
    private static final String EXIT_WITHOUT_SAVE_MESSAGE =
            "Your latest changes could not be saved.\nBye! Take care and see you again.";

    private final Path dataDirectory;
    private final InputStream input;
    private final Storage suppliedStorage;

    private Ui ui;
    private Scanner scanner;
    private Storage storage;
    private ActivityManager activityManager;
    private TopicManager topicManager;
    private FacilityManager facilityManager;
    private ConnectionManager connectionManager;
    private CommandDispatcher dispatcher;
    private CommandConfirmationHandler confirmationHandler;
    private boolean hasUnsavedChanges;

    /**
     * Creates an ApplicationRunner for one run of the application.
     *
     * @param dataDirectory the directory containing (or to create) application data files and,
     *     when recur is used, the externally supplied academic-calendar.txt
     * @param input the source of command-line input, e.g. System.in; never closed by this class
     */
    public ApplicationRunner(Path dataDirectory, InputStream input) {
        this(dataDirectory, input, null);
    }

    /**
     * Creates a runner with an injectable storage coordinator, for tests that need to observe or
     * force a storage failure (e.g. to verify recur/reset's in-memory rollback) without touching
     * the real filesystem. Production callers use {@link #ApplicationRunner(Path, InputStream)}.
     *
     * @param dataDirectory application data directory
     * @param input source of command-line input
     * @param suppliedStorage storage coordinator to use instead of constructing one from
     *     dataDirectory, or null to construct one normally
     */
    ApplicationRunner(Path dataDirectory, InputStream input, Storage suppliedStorage) {
        this.dataDirectory = dataDirectory;
        this.input = input;
        this.suppliedStorage = suppliedStorage;
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
        storage = suppliedStorage == null ? new Storage(dataDirectory) : suppliedStorage;
        activityManager = new ActivityManager();
        topicManager = new TopicManager(activityManager);
        confirmationHandler = new CommandConfirmationHandler(ui, scanner);

        if (!initialise()) {
            return;
        }

        dispatcher = new CommandDispatcher(activityManager, topicManager, facilityManager, connectionManager,
                storage);
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
            LoadResult<Topic> topicLoad = storage.loadTopics();
            LoadResult<Activity> activityLoad = storage.loadActivities(topicLoad.getRecords());
            LoadResult<Facility> facilityLoad = storage.loadFacilities();
            LoadResult<Connection> connectionLoad = storage.loadConnections(facilityLoad.getRecords());
            LoadResult<ActivityOrder> settingsLoad = storage.loadSettings();

            activityManager.loadAll(activityLoad.getRecords());
            topicManager.loadAll(topicLoad.getRecords());
            facilityManager = new FacilityManager(facilityLoad.getRecords());
            connectionManager = new ConnectionManager(connectionLoad.getRecords());
            activityManager.setDefaultOrder(settingsLoad.getRecords().get(0));

            ui.showLoadWarnings("activities.txt", activityLoad.getWarnings());
            ui.showLoadWarnings("topics.txt", topicLoad.getWarnings());
            ui.showLoadWarnings("facilities.txt", facilityLoad.getWarnings());
            ui.showLoadWarnings("connections.txt", connectionLoad.getWarnings());
            ui.showLoadWarnings("settings.txt", settingsLoad.getWarnings());
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
     * Dispatches, confirms if needed, and executes one line of input. A command that mutates
     * application state is persisted before its success feedback is shown, so a save failure is
     * reported instead of a false success message; a command that only reads state is never
     * saved. "bye" always ends the loop, attempting one final save first only if an earlier save
     * this session failed.
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
            ApplicationStateSnapshot snapshot = needsFailureRollback(command) ? new ApplicationStateSnapshot() : null;
            CommandResult result = command.execute();
            if (result.isShouldExit()) {
                return handleExit(result);
            }
            if (mutatesState(command)) {
                hasUnsavedChanges = true;
                if (!trySave()) {
                    if (snapshot != null) {
                        snapshot.restore();
                    }
                    return true;
                }
            }
            ui.showFramed(result.getFeedback());
            return true;
        } catch (UniEnableException e) {
            ui.showFramed("[Error] " + e.getErrorCategory() + ": " + e.getMessage());
            return true;
        }
    }

    /**
     * Handles an exit command ("bye"): always ends the command loop, but first attempts a final
     * save if an earlier command's save this session failed, so a session that hit a storage
     * error still gets one more chance to persist before exiting. Shows the normal goodbye
     * message only if nothing is left unsaved; otherwise shows the storage error and a farewell
     * that does not claim data was saved.
     *
     * @param result the exit command's own result, carrying the normal goodbye feedback
     * @return always false, so the command loop stops
     */
    private boolean handleExit(CommandResult result) {
        if (hasUnsavedChanges && !trySave()) {
            ui.showFramed(EXIT_WITHOUT_SAVE_MESSAGE);
        } else {
            ui.showFramed(result.getFeedback());
        }
        return false;
    }

    /**
     * Returns whether executing the given command may have changed activity, topic, or
     * settings state that needs persisting. Read-only commands (list, find, view, next, guide,
     * facility/connection lookups, order view, bye) are deliberately excluded so they never
     * trigger a save.
     *
     * @param command the already-executed command
     * @return true if command is one of the mutating command types
     */
    private boolean mutatesState(Command command) {
        return command instanceof AddCommand
                || command instanceof DeleteCommand
                || command instanceof EditCommand
                || command instanceof MarkCommand
                || command instanceof UnmarkCommand
                || command instanceof OrderSetCommand
                || command instanceof TopicAddCommand
                || command instanceof TopicRenameCommand
                || command instanceof TopicDeleteCommand
                || command instanceof RecurCommand
                || command instanceof ResetCommand;
    }

    /**
     * Returns whether a failed save must roll back this command's complete in-memory batch, not
     * just be reported. Every other mutating command changes at most one activity/topic, so a
     * failed save leaving that one change in memory (until the next successful save or restart
     * reloads from disk) is accepted, existing behaviour; recur can add many activities in one
     * command and reset can rewrite the whole collection, so a failed save must not leave a
     * partially-applied batch sitting in memory only.
     *
     * @param command the about-to-execute command
     * @return true if a failed save after this command must restore the pre-command state
     */
    private boolean needsFailureRollback(Command command) {
        return command instanceof RecurCommand || command instanceof ResetCommand;
    }

    /** Exact pre-command state used to roll back recur/reset when persistence fails. */
    private final class ApplicationStateSnapshot {
        private final List<Activity> activities = List.copyOf(activityManager.getAll());
        private final List<Topic> topics = List.copyOf(topicManager.getAll());
        private final int nextId = activityManager.getNextId();
        private final ActivityOrder order = activityManager.getDefaultOrder();
        private final boolean previouslyUnsaved = hasUnsavedChanges;

        private void restore() {
            activityManager.restoreState(activities, nextId, order);
            topicManager.loadAll(topics);
            hasUnsavedChanges = previouslyUnsaved;
        }
    }

    /**
     * Attempts to persist activities, topics, and the saved default activity order, updating the
     * unsaved-changes flag to match the outcome. On failure, shows the storage error itself so
     * callers only need to react to whether the save succeeded.
     *
     * @return true if the save succeeded; false if it failed (the error has already been shown)
     */
    private boolean trySave() {
        try {
            storage.saveAll(activityManager.getAll(), topicManager.getAll(), activityManager.getDefaultOrder());
            hasUnsavedChanges = false;
            return true;
        } catch (StorageException e) {
            hasUnsavedChanges = true;
            ui.showFramed("[Error] " + e.getErrorCategory() + ": " + e.getMessage());
            return false;
        }
    }
}
