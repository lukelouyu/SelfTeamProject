package seedu.unienable.command.accessibility.connection;

import java.util.List;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.accessibility.common.ValidationReportFormatter;
import seedu.unienable.exception.StorageException;
import seedu.unienable.storage.Storage;

/**
 * Re-runs connections.txt's own load-time integrity checks on demand and reports every issue
 * found, line by line - including endpoint names that do not match a known facility in
 * facilities.txt. Read-only: this never changes connections.txt, and never replaces the
 * already-loaded ConnectionManager the rest of the running application is using.
 */
public class ConnectionValidateCommand extends Command {
    private final Storage storage;

    /**
     * Creates a ConnectionValidateCommand.
     *
     * @param storage the storage coordinator to re-read facilities.txt and connections.txt through
     */
    public ConnectionValidateCommand(Storage storage) {
        this.storage = storage;
    }

    @Override
    public CommandResult execute() throws StorageException {
        List<Facility> knownFacilities = storage.loadFacilities().getRecords();
        List<String> warnings = storage.loadConnections(knownFacilities).getWarnings();
        return new CommandResult(ValidationReportFormatter.format("connections.txt", warnings));
    }
}
