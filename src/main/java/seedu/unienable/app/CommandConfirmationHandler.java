package seedu.unienable.app;

import java.util.Objects;
import java.util.Scanner;

import seedu.unienable.command.Command;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;
import seedu.unienable.command.MenuConfirmable;
import seedu.unienable.command.MenuOutcome;
import seedu.unienable.exception.UniEnableException;
import seedu.unienable.ui.Ui;

/**
 * Decides whether a dispatched command needs confirmation before it may execute, and carries out
 * that confirmation: showing the command-specific preview, reading the next input line as the
 * answer, and reporting cancellation. Any command opts into a plain y/n confirmation generically
 * by implementing {@link Confirmable}; a command needing a numbered menu with more than one
 * meaningful choice (e.g. reset's three options) implements {@link MenuConfirmable} instead. A
 * command implementing neither proceeds without asking. Adding a new confirmable command - of
 * either shape - never requires changing this class.
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
     * @return true if the command needs no confirmation (or implements neither confirmation
     *     interface), or the answer selected proceeding (y/n's "y", or a menu's non-cancelling
     *     choice); false if the command was cancelled outright, or the answer meant cancel or was
     *     unrecognised (including EOF)
     * @throws UniEnableException if building the command's confirmation preview fails, e.g. it
     *     references an activity ID that does not exist
     */
    public boolean confirmIfNeeded(Command command) throws UniEnableException {
        if (command instanceof MenuConfirmable) {
            return confirmMenu((MenuConfirmable) command);
        }
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

    private boolean confirmMenu(MenuConfirmable command) throws UniEnableException {
        String prompt = Objects.requireNonNull(command.getMenuPrompt(),
                "MenuConfirmable.getMenuPrompt() must not return null - see its Javadoc "
                        + "(DEFECT-01): a menu-confirmable command must always show its menu, "
                        + "never silently auto-confirm because a prompt was absent.");
        ui.showFramed(prompt);
        String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        MenuOutcome outcome = command.applyMenuAnswer(answer);
        if (outcome.isProceed()) {
            return true;
        }
        ui.showFramed(outcome.getMessage());
        return false;
    }
}
