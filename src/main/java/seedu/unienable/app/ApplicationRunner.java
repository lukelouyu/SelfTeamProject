package seedu.unienable.app;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;
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
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.preference.PreferenceProfile;
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
    private static final Logger LOGGER = Logger.getLogger(ApplicationRunner.class.getName());
    private static final String EXIT_WITHOUT_SAVE_MESSAGE =
            "Your latest changes could not be saved.\nBye! Take care and see you again.";

    private final Path dataDirectory;
    private final InputStream input;
    private final Storage suppliedStorage;
    private final Supplier<LocalDateTime> nowSupplier;

    private Ui ui;
    private Scanner scanner;
    private Storage storage;
    private ActivityManager activityManager;
    private TopicManager topicManager;
    private FacilityManager facilityManager;
    private ConnectionManager connectionManager;
    private PreferenceManager preferenceManager;
    private RecommendationManager recommendationManager;
    private CommandTransactionExecutor transactionExecutor;
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
        this(dataDirectory, input, null, LocalDateTime::now);
    }

    /**
     * Creates a runner with an injectable time source for deterministic tests and scripted
     * regression batches.
     *
     * @param dataDirectory the directory containing (or to create) application data files and,
     *     when recur is used, the externally supplied academic-calendar.txt
     * @param input the source of command-line input, e.g. System.in; never closed by this class
     * @param nowSupplier the current-time source to use for command dispatch
     */
    public ApplicationRunner(Path dataDirectory, InputStream input, Supplier<LocalDateTime> nowSupplier) {
        this(dataDirectory, input, null, nowSupplier);
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
        this(dataDirectory, input, suppliedStorage, LocalDateTime::now);
    }

    ApplicationRunner(Path dataDirectory, InputStream input, Storage suppliedStorage,
            Supplier<LocalDateTime> nowSupplier) {
        this.dataDirectory = dataDirectory;
        this.input = input;
        this.suppliedStorage = suppliedStorage;
        this.nowSupplier = nowSupplier;
    }

    /**
     * Runs the full application: shows the welcome message, loads saved data, then processes
     * commands until "bye" or the input stream ends. If startup data loading fails, shows the
     * error and returns without starting the command loop. Always releases this run's log file
     * handle before returning (even on an early return or an uncaught exception), so the data
     * directory - including the log file - is free to be deleted or reopened immediately after.
     */
    public void run() {
        LoggingConfig.configure(dataDirectory);
        try {
            runInternal();
        } finally {
            LoggingConfig.shutdown();
        }
    }

    private void runInternal() {
        ui = new Ui();
        ui.showWelcome();

        scanner = new Scanner(input);
        storage = suppliedStorage == null ? new Storage(dataDirectory) : suppliedStorage;
        activityManager = new ActivityManager();
        topicManager = new TopicManager(activityManager);
        preferenceManager = new PreferenceManager();
        recommendationManager = new RecommendationManager();
        confirmationHandler = new CommandConfirmationHandler(ui, scanner);

        if (!initialise()) {
            return;
        }

        transactionExecutor = new CommandTransactionExecutor(
                activityManager, topicManager, preferenceManager);
        dispatcher = new CommandDispatcher(activityManager, topicManager, facilityManager,
                connectionManager, preferenceManager, recommendationManager, storage);
        runCommandLoop();
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
            LoadResult<PreferenceProfile> preferencesLoad = storage.loadPreferences();

            activityManager.loadAll(activityLoad.getRecords());
            topicManager.loadAll(topicLoad.getRecords());
            facilityManager = new FacilityManager(facilityLoad.getRecords());
            connectionManager = new ConnectionManager(connectionLoad.getRecords());
            activityManager.setDefaultOrder(settingsLoad.getRecords().get(0));
            preferenceManager.setProfile(preferencesLoad.getRecords().get(0));

            showAndLogLoadWarnings("activities.txt", activityLoad.getWarnings());
            showAndLogLoadWarnings("topics.txt", topicLoad.getWarnings());
            showAndLogLoadWarnings("facilities.txt", facilityLoad.getWarnings());
            showAndLogLoadWarnings("connections.txt", connectionLoad.getWarnings());
            showAndLogLoadWarnings("settings.txt", settingsLoad.getWarnings());
            showAndLogLoadWarnings("preferences.txt", preferencesLoad.getWarnings());
            return true;
        } catch (UniEnableException e) {
            LOGGER.log(Level.WARNING, "Storage load failure during startup", e);
            showStartupError(e);
            return false;
        }
    }

    /**
     * Shows a data file's partial-load warnings on the CLI, and separately logs how many records
     * were skipped (a count only, not the warning text itself, which can echo back raw file
     * content) so a malformed data file is diagnosable from the log without waiting for a user
     * report.
     *
     * @param fileName the data file the warnings came from
     * @param warnings the warnings produced while loading it
     */
    private void showAndLogLoadWarnings(String fileName, List<String> warnings) {
        ui.showLoadWarnings(fileName, warnings);
        if (!warnings.isEmpty()) {
            LOGGER.warning(fileName + ": " + warnings.size() + " malformed record(s) skipped on load.");
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
     * application state has a complete pre-execution snapshot captured, is persisted before its
     * success feedback is shown, and has the snapshot restored if execution or saving fails. A
     * failure is therefore reported instead of a false success message and never leaves a
     * partially-applied change sitting in memory only; a command that only reads state is never
     * snapshotted or saved. "bye" always ends the loop, attempting one final save first only if
     * an earlier save this session failed.
     *
     * @param line one full line of raw user input
     * @return true if the command loop should keep reading input; false if this command should
     *     end the application (e.g. "bye")
     */
    private boolean processCommand(String line) {
        assert dispatcher != null && confirmationHandler != null && transactionExecutor != null
                && activityManager != null
                && topicManager != null && ui != null
                : "processCommand requires run() to have completed its setup first";
        try {
            Command command = dispatcher.dispatch(line, nowSupplier.get());
            if (!confirmationHandler.confirmIfNeeded(command)) {
                return true;
            }
            CommandTransactionExecutor.Execution execution = transactionExecutor.execute(command);
            CommandResult result = execution.getResult();
            if (result.isShouldExit()) {
                return handleExit(result);
            }
            if (execution.hasStateChange()) {
                hasUnsavedChanges = true;
                if (!trySave()) {
                    execution.rollback();
                    return true;
                }
                recommendationManager.clear();
            }
            ui.showFramed(result.getFeedback());
            return true;
        } catch (UniEnableException e) {
            ui.showFramed("[Error] " + e.getErrorCategory() + ": " + e.getMessage());
            return true;
        } catch (RuntimeException e) {
            // Last-resort catch at the application boundary. The pre-execution snapshot has
            // already been restored by the transaction executor, so an unexpected bug is logged and
            // reported without crashing the command loop or retaining partial state.
            LOGGER.log(Level.SEVERE, "Unexpected internal error while processing command: " + line, e);
            ui.showFramed("[Error] An unexpected internal error occurred. Enter guide for help.");
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
     * Attempts to persist activities, topics, the saved default activity order, and preferences,
     * updating the unsaved-changes flag to match the outcome. On failure, shows the storage error
     * itself so callers only need to react to whether the save succeeded.
     *
     * @return true if the save succeeded; false if it failed (the error has already been shown)
     */
    private boolean trySave() {
        try {
            storage.saveAll(activityManager.getAll(), topicManager.getAll(),
                    activityManager.getDefaultOrder(), preferenceManager.getProfile());
            hasUnsavedChanges = false;
            return true;
        } catch (StorageException e) {
            hasUnsavedChanges = true;
            LOGGER.log(Level.WARNING, "Storage save failure", e);
            ui.showFramed("[Error] " + e.getErrorCategory() + ": " + e.getMessage());
            return false;
        }
    }
}
