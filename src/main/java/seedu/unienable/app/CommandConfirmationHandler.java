package seedu.unienable.app;

import java.util.Scanner;

import seedu.unienable.command.Command;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;
import seedu.unienable.exception.UniEnableException;
import seedu.unienable.ui.Ui;

/**
 * Decides whether a dispatched command needs a y/n confirmation before it may execute, and
 * carries out that confirmation: showing the command-specific preview, reading the next input
 * line as the answer, and reporting cancellation. Any command opts into this generically by
 * implementing {@link Confirmable}; a command that does not implement it proceeds without asking.
 * Adding a new confirmable command never requires changing this class - each command supplies its
 * own {@link Confirmation} via {@link Confirmable#getConfirmation()}.
 */
public class CommandConfirmationHandler {
    private final Ui ui;
    private final Scanner scanner;

    /**
     * Creates a CommandConfirmationHandler.
     *
     * @param ui the UI to show previews and prompts through
     * @param scanner the input source to read the y/n answer from; never closed by this class
     */
    public CommandConfirmationHandler(Ui ui, Scanner scanner) {
        this.ui = ui;
        this.scanner = scanner;
    }

    /**
     * Shows a confirmation preview and prompt if the given command requires one, and reports
     * whether execution should proceed.
     *
     * @param command the dispatched command, not yet executed
     * @return true if the command needs no confirmation (or is not {@link Confirmable} at all),
     *     or the user answered "y"/"Y" to a y/n prompt; false if the command was cancelled
     *     outright, or the user answered anything else (including EOF) to a y/n prompt
     * @throws UniEnableException if building the command's confirmation preview fails, e.g. it
     *     references an activity ID that does not exist
     */
    public boolean confirmIfNeeded(Command command) throws UniEnableException {
        if (!(command instanceof Confirmable)) {
            return true;
        }
        Confirmation confirmation = ((Confirmable) command).getConfirmation();
        if (confirmation.isProceed()) {
            return true;
        }
        if (confirmation.isCancel()) {
            ui.showFramed(confirmation.getMessage());
            return false;
        }
        return confirm(confirmation.getMessage());
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
