package seedu.unienable.command.accessibility.facility;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.accessibility.common.AccessibilityDisclaimer;

import java.util.List;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.classes.FacilityFeature;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.logic.FacilityManager;

/** Finds every facility with a given accessibility feature at a given status. */
public class FacilityFindCommand extends Command {
    private final FacilityManager facilityManager;
    private final FacilityFeature.Type type;
    private final AccessibilityStatus status;

    /**
     * Creates a FacilityFindCommand.
     *
     * @param facilityManager the manager to search
     * @param type the feature type to filter on
     * @param status the required status for that feature (defaults to YES when unspecified by the parser)
     */
    public FacilityFindCommand(FacilityManager facilityManager, FacilityFeature.Type type,
            AccessibilityStatus status) {
        this.facilityManager = facilityManager;
        this.type = type;
        this.status = status;
    }

    @Override
    public CommandResult execute() {
        return new CommandResult(formatResult(facilityManager.findByFeature(type, status)));
    }

    private String formatResult(List<Facility> facilities) {
        StringBuilder result = new StringBuilder("Facilities where ").append(type).append(" is ").append(status)
                .append(':');
        int nameWidth = maxNameWidth(facilities);
        for (Facility facility : facilities) {
            result.append('\n').append(formatFacilityLine(facility, nameWidth));
        }
        if (status == AccessibilityStatus.UNKNOWN) {
            result.append("\n\nUNKNOWN means the local dataset does not confirm the feature.");
        }
        result.append("\n\n").append(AccessibilityDisclaimer.TEXT);
        return result.toString();
    }

    private String formatFacilityLine(Facility facility, int nameWidth) {
        String notes = findMatchingNotes(facility);
        StringBuilder line = new StringBuilder("[").append(facility.getId()).append("] ");
        if (notes != null) {
            line.append(String.format("%-" + nameWidth + "s", facility.getName())).append(" | ").append(notes);
        } else {
            line.append(facility.getName());
        }
        return line.toString();
    }

    private String findMatchingNotes(Facility facility) {
        for (FacilityFeature feature : facility.getFeatures()) {
            if (feature.getType() == type && feature.getStatus() == status) {
                return feature.getNotes();
            }
        }
        return null;
    }

    private int maxNameWidth(List<Facility> facilities) {
        int max = 0;
        for (Facility facility : facilities) {
            max = Math.max(max, facility.getName().length());
        }
        return max;
    }
}
