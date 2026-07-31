package seedu.unienable.app;

import java.util.Scanner;

import seedu.unienable.command.Command;
import seedu.unienable.command.activity.DeleteCommand;
import seedu.unienable.command.activity.EditCommand;
import seedu.unienable.command.general.ResetCommand;
import seedu.unienable.command.topic.TopicDeleteCommand;
import seedu.unienable.command.topic.TopicRenameCommand;
import seedu.unienable.exception.UniEnableException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.ui.MessageFormatter;
import seedu.unienable.ui.Ui;

/**
 * Decides whether a dispatched command needs a y/n confirmation before it may execute, and
 * carries out that confirmation: showing the command-specific preview, reading the next input
 * line as the answer, and reporting cancellation. Confirmation is required for
 * {@link DeleteCommand}, {@link EditCommand}, {@link TopicRenameCommand},
 * {@link TopicDeleteCommand}, and {@link ResetCommand} (unless there is nothing to reset); every
 * other command proceeds without asking.
 */
public class CommandConfirmationHandler {
    private final Ui ui;
    private final Scanner scanner;
    private final ActivityManager activityManager;

    /**
     * Creates a CommandConfirmationHandler.
     *
     * @param ui the UI to show previews and prompts through
     * @param scanner the input source to read the y/n answer from; never closed by this class
     * @param activityManager the manager consulted to preview delete/edit commands
     */
    public CommandConfirmationHandler(Ui ui, Scanner scanner, ActivityManager activityManager) {
        this.ui = ui;
        this.scanner = scanner;
        this.activityManager = activityManager;
    }

    /**
     * Shows a confirmation preview and prompt if the given command requires one, and reports
     * whether execution should proceed.
     *
     * @param command the dispatched command, not yet executed
     * @return true if the command needs no confirmation, or the user answered "y"/"Y"; false if
     *     the user cancelled (any other answer, including EOF), or - for an edit whose requested
     *     fields all already match the stored activity - confirmation was skipped because there
     *     is nothing to save
     * @throws UniEnableException if a delete or edit references an activity ID that does not exist
     */
    public boolean confirmIfNeeded(Command command) throws UniEnableException {
        if (command instanceof DeleteCommand) {
            return confirmDelete((DeleteCommand) command);
        }
        if (command instanceof EditCommand) {
            return confirmEdit((EditCommand) command);
        }
        if (command instanceof TopicRenameCommand) {
            return confirmTopicRename((TopicRenameCommand) command);
        }
        if (command instanceof TopicDeleteCommand) {
            return confirmTopicDelete((TopicDeleteCommand) command);
        }
        if (command instanceof ResetCommand) {
            return confirmReset((ResetCommand) command);
        }
        return true;
    }

    private boolean confirmDelete(DeleteCommand delete) throws UniEnableException {
        Activity activity = activityManager.getById(delete.getId());
        return confirm("You selected activity [" + delete.getId() + "]:\n"
                + MessageFormatter.formatConcise(activity) + "\n\nDelete this activity? (y/n)");
    }

    private boolean confirmEdit(EditCommand edit) throws UniEnableException {
        Activity oldActivity = activityManager.getById(edit.getId());
        String diff = MessageFormatter.formatChanges(oldActivity, edit.getNewActivity());
        if (diff.isEmpty()) {
            ui.showFramed("No changes to activity [" + edit.getId() + "].");
            return false;
        }
        return confirm(diff + "\nSave changes? (y/n)");
    }

    private boolean confirmTopicRename(TopicRenameCommand rename) {
        String diff = "Before: topic = " + rename.getOldName() + "\nAfter : topic = " + rename.getNewName();
        return confirm(diff + "\nSave changes? (y/n)");
    }

    private boolean confirmTopicDelete(TopicDeleteCommand delete) {
        return confirm("Delete topic \"" + delete.getName() + "\" under " + delete.getCategory() + "? (y/n)");
    }

    private boolean confirmReset(ResetCommand reset) {
        if (!reset.hasAnythingToReset()) {
            return true;
        }
        String preview = "Reset all user data?\n\n"
                + "Activities to delete: " + reset.getActivityCount() + "\n"
                + "Topics to delete   : " + reset.getTopicCount() + "\n"
                + "Default order      : reset to chronological\n\n"
                + "Facility and connection reference data will be kept.\n"
                + "This action cannot be undone.\n"
                + "Continue? (y/n)";
        return confirm(preview);
    }

    private boolean confirm(String prompt) {
        ui.showFramed(prompt);
        String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "n";
        if ("y".equalsIgnoreCase(answer)) {
            return true;
        }
        ui.showFramed("Cancelled. No changes were made.");
        return false;
    }
}
