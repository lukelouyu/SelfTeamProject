package seedu.unienable.parser.accessibility;

import java.util.Map;

import seedu.unienable.command.accessibility.route.RouteCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.parser.common.FieldParser;

/** Parses {@code route from/FACILITY to/FACILITY} into a {@link RouteCommand}. */
public class RouteCommandParser {
    private static final String[] ROUTE_MARKERS = { "from/", "to/" };

    /**
     * Parses a route command's argument text. {@code from/} and {@code to/} may appear in either
     * order (matching {@code connection find}'s marker parsing), and both are required.
     *
     * @param facilityManager the manager the resulting command will route over
     * @param connectionManager the manager the resulting command will route over
     * @param args the text after the "route" command word
     * @return the parsed RouteCommand
     * @throws MissingInputException if {@code from/} or {@code to/} is absent or blank
     * @throws InvalidCommandException if unrecognised leading text is present, or {@code from/}
     *     or {@code to/} is supplied more than once
     */
    public RouteCommand parse(FacilityManager facilityManager, ConnectionManager connectionManager, String args)
            throws MissingInputException, InvalidCommandException {
        FieldParser.rejectUnrecognisedLeadingText(args, ROUTE_MARKERS);
        FieldParser.rejectDuplicateMarkers(args, ROUTE_MARKERS);
        Map<String, String> fields = FieldParser.extractPresentFields(args, ROUTE_MARKERS);
        String from = fields.get("from/");
        String to = fields.get("to/");
        if (from == null || from.isEmpty()) {
            throw new MissingInputException("a source facility (from/) is required.");
        }
        if (to == null || to.isEmpty()) {
            throw new MissingInputException("a destination facility (to/) is required.");
        }
        return new RouteCommand(facilityManager, connectionManager, from, to);
    }
}
