package seedu.unienable.command.accessibility;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import java.util.List;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.classes.FacilityFeature;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.FacilityManager;

/** Displays one facility's every recorded accessibility feature. */
public class FacilityViewCommand extends Command {
    private final FacilityManager facilityManager;
    private final String name;

    public FacilityViewCommand(FacilityManager facilityManager, String name) {
        this.facilityManager = facilityManager;
        this.name = name;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        return new CommandResult(formatResult(facilityManager.findByName(name)));
    }

    private String formatResult(Facility facility) {
        List<FacilityFeature> features = facility.getFeatures();
        int typeWidth = maxTypeWidth(features);
        int statusWidth = maxStatusWidth(features);

        StringBuilder result = new StringBuilder("Facility: ").append(facility.getName())
                .append(" [").append(facility.getId()).append(']');
        for (FacilityFeature feature : features) {
            result.append('\n').append(formatFeatureLine(feature, typeWidth, statusWidth));
        }
        result.append("\n\n").append(AccessibilityDisclaimer.TEXT);
        return result.toString();
    }

    private String formatFeatureLine(FacilityFeature feature, int typeWidth, int statusWidth) {
        StringBuilder line = new StringBuilder();
        line.append(String.format("%-" + typeWidth + "s", feature.getType())).append(" | ");
        if (feature.getNotes() != null) {
            line.append(String.format("%-" + statusWidth + "s", feature.getStatus()))
                    .append(" | ").append(feature.getNotes());
        } else {
            line.append(feature.getStatus());
        }
        return line.toString();
    }

    private int maxTypeWidth(List<FacilityFeature> features) {
        int max = 0;
        for (FacilityFeature feature : features) {
            max = Math.max(max, feature.getType().name().length());
        }
        return max;
    }

    private int maxStatusWidth(List<FacilityFeature> features) {
        int max = 0;
        for (FacilityFeature feature : features) {
            max = Math.max(max, feature.getStatus().name().length());
        }
        return max;
    }
}
