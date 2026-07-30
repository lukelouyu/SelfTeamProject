package seedu.unienable.command;

import java.util.List;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.logic.FacilityManager;

/** Lists every known facility's stable ID and name. */
public class FacilityListCommand extends Command {
    private final FacilityManager facilityManager;

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
        result.append("\n\nThis is local reference data, not a real-time guarantee.");
        return new CommandResult(result.toString());
    }
}
