package seedu.unienable.command.accessibility.facility;

import java.util.List;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.accessibility.common.ValidationReportFormatter;
import seedu.unienable.exception.StorageException;
import seedu.unienable.storage.Storage;

/**
 * Re-runs facilities.txt's own load-time integrity checks on demand and reports every issue found,
 * line by line. Read-only: this never changes facilities.txt, and never replaces the already-loaded
 * FacilityManager the rest of the running application is using. Lets a user who hand-edits
 * facilities.txt self-check it for mistakes without restarting the application or having to notice
 * the one-time warnings shown at startup.
 */
public class FacilityValidateCommand extends Command {
    private final Storage storage;

    /**
     * Creates a FacilityValidateCommand.
     *
     * @param storage the storage coordinator to re-read facilities.txt through
     */
    public FacilityValidateCommand(Storage storage) {
        this.storage = storage;
    }

    @Override
    public CommandResult execute() throws StorageException {
        List<String> warnings = storage.loadFacilities().getWarnings();
        return new CommandResult(ValidationReportFormatter.format("facilities.txt", warnings));
    }
}
