package seedu.unienable.accessibility;

/** A two-way route connection between two known facilities. Read-only reference data. */
public class Connection {
    private final int id;
    private final String from;
    private final String to;
    private final int distanceInMetres;
    private final AccessibilityStatus accessibility;
    private final TraversalType type;
    private final ShelterStatus shelter;
    private final String knownBarrier;
    private final String notes;

    /**
     * Creates a Connection. Every connection is treated as two-way.
     *
     * @param id stable connection ID
     * @param from one endpoint facility name
     * @param to the other endpoint facility name
     * @param distanceInMetres distance in metres
     * @param accessibility whether the connection is confirmed accessible, confirmed inaccessible, or unknown
     * @param type how the connection is physically travelled
     * @param shelter whether the connection is confirmed sheltered, confirmed unsheltered, or unknown
     * @param knownBarrier a recorded barrier, or null if none
     * @param notes optional context notes, or null if none
     */
    public Connection(int id, String from, String to, int distanceInMetres, AccessibilityStatus accessibility,
            TraversalType type, ShelterStatus shelter, String knownBarrier, String notes) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.distanceInMetres = distanceInMetres;
        this.accessibility = accessibility;
        this.type = type;
        this.shelter = shelter;
        this.knownBarrier = knownBarrier;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public int getDistanceInMetres() {
        return distanceInMetres;
    }

    public AccessibilityStatus getAccessibility() {
        return accessibility;
    }

    public TraversalType getType() {
        return type;
    }

    public ShelterStatus getShelter() {
        return shelter;
    }

    public String getKnownBarrier() {
        return knownBarrier;
    }

    public String getNotes() {
        return notes;
    }
}
