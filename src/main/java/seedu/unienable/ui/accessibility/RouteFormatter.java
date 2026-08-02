package seedu.unienable.ui.accessibility;

import java.util.List;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.command.accessibility.common.AccessibilityDisclaimer;
import seedu.unienable.logic.graph.GraphPath;

/**
 * Formats a {@code route} command's result into user-facing text. Pure formatting only - every
 * value shown here (the path, its segments, whether a route exists at all) is decided by
 * {@code RouteCommand}; this class makes no routing or eligibility decisions of its own.
 */
public final class RouteFormatter {
    private static final String LIMITATIONS_NOTE = "This is a confirmed-accessible route based on "
            + "local data only. It does not estimate travel time and does not guarantee real-world "
            + "accessibility at the time of travel.";

    private RouteFormatter() {
    }

    /**
     * Formats a found route: every segment travelled, in order, plus the total distance. Handles
     * the zero-segment same-facility case (an empty {@code segments} list) as a successful result,
     * not an error.
     *
     * @param path the shortest confirmed-accessible path found
     * @param segments the connection backing each consecutive pair in {@code path}'s facility
     *     chain, in travel order; empty if source and destination are the same facility
     * @return the formatted route report
     */
    public static String formatRoute(GraphPath path, List<Connection> segments) {
        List<String> facilityNames = path.getFacilityNames();
        StringBuilder result = new StringBuilder("Route from ").append(facilityNames.get(0))
                .append(" to ").append(facilityNames.get(facilityNames.size() - 1)).append(":\n");
        if (segments.isEmpty()) {
            result.append("Source and destination are the same facility. No travel is required.\n");
        } else {
            for (int i = 0; i < segments.size(); i++) {
                result.append(formatSegmentLine(i + 1, facilityNames.get(i), facilityNames.get(i + 1),
                        segments.get(i))).append('\n');
            }
        }
        result.append("Total distance: ").append(path.getTotalDistanceInMetres()).append(" m (")
                .append(segments.size()).append(segments.size() == 1 ? " segment)" : " segments)")
                .append("\n\n").append(LIMITATIONS_NOTE).append("\n\n").append(AccessibilityDisclaimer.TEXT);
        return result.toString();
    }

    /**
     * Formats the no-route fallback for two known facilities with no confirmed-accessible path
     * connecting them. Wording is deliberately scoped to UniEnable's own local dataset - it must
     * never be read as a claim that no real-world accessible route exists.
     *
     * @param from the normalised (canonical-case) source facility name
     * @param to the normalised (canonical-case) destination facility name
     * @return the formatted fallback message
     */
    public static String formatNoRoute(String from, String to) {
        return "No supported accessible route was found between \"" + from + "\" and \"" + to
                + "\" in UniEnable's local dataset.\n"
                + "This does not mean no real-world accessible route exists - only that one is not "
                + "confirmed here.\n"
                + "Please ask people around you.\n"
                + "We are still improving our database.\n\n"
                + AccessibilityDisclaimer.TEXT;
    }

    private static String formatSegmentLine(int index, String from, String to, Connection connection) {
        StringBuilder line = new StringBuilder("[").append(index).append("] ").append(from).append(" -> ")
                .append(to).append(" | ").append(connection.getDistanceInMetres()).append(" m | ")
                .append(connection.getType()).append(" | Shelter: ").append(connection.getShelter());
        if (connection.getKnownBarrier() != null) {
            line.append(" | Barrier: ").append(connection.getKnownBarrier());
        }
        if (connection.getNotes() != null) {
            line.append(" | Notes: ").append(connection.getNotes());
        }
        return line.toString();
    }
}
