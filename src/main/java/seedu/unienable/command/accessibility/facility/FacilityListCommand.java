package seedu.unienable.command.accessibility.facility;

import seedu.unienable.command.ReadOnlyCommand;
import seedu.unienable.command.CommandResult;
import seedu.unienable.ui.accessibility.AccessibilityDisclaimer;

import java.util.List;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.logic.FacilityManager;

/** Lists every known facility's stable ID and name. */
public class FacilityListCommand extends ReadOnlyCommand {
    private final FacilityManager facilityManager;

    /**
     * Creates a FacilityListCommand.
     *
     * @param facilityManager the manager to list facilities from
     */
    public FacilityListCommand(FacilityManager facilityManager) {
        this.facilityManager = facilityManager;
    }

    @Override
    public CommandResult execute() {
        List<Facility> facilities = facilityManager.list();
        StringBuilder result = new StringBuilder("Known facilities in the local reference:");
        for (Facility facility : facilities) {
            result.append("\n[").append(facility.getId()).append("] ").append(facility.getName());
        }
        result.append("\n\n").append(AccessibilityDisclaimer.TEXT);
        return new CommandResult(result.toString());
    }
}
