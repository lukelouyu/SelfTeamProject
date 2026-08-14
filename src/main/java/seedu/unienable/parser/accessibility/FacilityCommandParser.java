package seedu.unienable.parser.accessibility;

import java.util.Locale;

import seedu.unienable.accessibility.classes.FacilityFeature;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.command.accessibility.facility.FacilityFindCommand;
import seedu.unienable.command.accessibility.facility.FacilityListCommand;
import seedu.unienable.command.accessibility.facility.FacilityValidateCommand;
import seedu.unienable.command.accessibility.facility.FacilityViewCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.parser.common.FieldParser;
import seedu.unienable.storage.Storage;

/** Parses facility-related commands (facility list, facility view, facility find) into Command objects. */
public class FacilityCommandParser {
    /**
     * Builds a FacilityListCommand. "facility list" takes no arguments.
     *
     * @param facilityManager the manager the resulting command will read from
     * @param args the text after the "facility list" command words, expected to be empty
     * @return the parsed FacilityListCommand
     * @throws InvalidCommandException if args is not empty
     */
    public FacilityListCommand parseList(FacilityManager facilityManager, String args)
            throws InvalidCommandException {
        FieldParser.requireNoArguments("facility list", args);
        return new FacilityListCommand(facilityManager);
    }

    /**
     * Parses a facility view command's argument text into a FacilityViewCommand.
     *
     * @param facilityManager the manager the resulting command will read from
     * @param args the text after the "facility view" command words: the facility name
     * @return the parsed FacilityViewCommand
     * @throws MissingInputException if no facility name is supplied
     */
    public FacilityViewCommand parseView(FacilityManager facilityManager, String args) throws MissingInputException {
        String name = args.trim();
        if (name.isEmpty()) {
            throw new MissingInputException("a facility name is required.");
        }
        return new FacilityViewCommand(facilityManager, name);
    }

    /**
     * Parses a facility find command's argument text into a FacilityFindCommand. If status/ is
     * not supplied, YES is used.
     *
     * @param facilityManager the manager the resulting command will read from
     * @param args the text after the "facility find" command words
     * @return the parsed FacilityFindCommand
     * @throws MissingInputException if type/ is missing
     * @throws InvalidCommandException if type or status is invalid, or either marker is supplied
     *     more than once
     */
    public FacilityFindCommand parseFind(FacilityManager facilityManager, String args)
            throws MissingInputException, InvalidCommandException {
        FieldParser.rejectUnrecognisedLeadingText(args, "type/", "status/");
        FieldParser.rejectDuplicateMarkers(args, "type/", "status/");
        boolean hasStatus = FieldParser.indexOfMarker(args, "status/", 0) != -1;
        String typeEndMarker = hasStatus ? "status/" : null;
        String typeText = FieldParser.extractField(args, "type/", typeEndMarker);
        if (typeText == null || typeText.isEmpty()) {
            throw new MissingInputException("type/ is required.");
        }
        FacilityFeature.Type type = parseType(typeText);
        AccessibilityStatus status = hasStatus
                ? FieldParser.parseAccessibilityStatus(FieldParser.extractField(args, "status/", null))
                : AccessibilityStatus.YES;
        return new FacilityFindCommand(facilityManager, type, status);
    }

    /**
     * Builds a FacilityValidateCommand. "facility validate" takes no arguments.
     *
     * @param storage the storage coordinator the resulting command will re-read facilities.txt through
     * @param args the text after the "facility validate" command words, expected to be empty
     * @return the parsed FacilityValidateCommand
     * @throws InvalidCommandException if args is not empty
     */
    public FacilityValidateCommand parseValidate(Storage storage, String args) throws InvalidCommandException {
        FieldParser.requireNoArguments("facility validate", args);
        return new FacilityValidateCommand(storage);
    }

    private FacilityFeature.Type parseType(String text) throws InvalidCommandException {
        try {
            return FacilityFeature.Type.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("type must be one of LIFT, RAMP, SHELTERED_RAMP, "
                    + "ACCESSIBLE_WASHROOM, STEP_FREE_ENTRANCE, REST_POINT, AUTOMATIC_DOOR, OTHER.");
        }
    }
}
