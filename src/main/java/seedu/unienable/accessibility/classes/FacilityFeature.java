package seedu.unienable.accessibility.classes;

import seedu.unienable.accessibility.enums.AccessibilityStatus;

/** One accessibility feature recorded for a facility, such as a lift or ramp. Read-only reference data. */
public class FacilityFeature {
    private final Type type;
    private final AccessibilityStatus status;
    private final String notes;

    /**
     * Creates a FacilityFeature.
     *
     * @param type the feature type
     * @param status whether the feature is confirmed present, confirmed absent, or unknown
     * @param notes optional location or context notes, or null if none
     */
    public FacilityFeature(Type type, AccessibilityStatus status, String notes) {
        this.type = type;
        this.status = status;
        this.notes = notes;
    }

    /** Returns the feature type. */
    public Type getType() {
        return type;
    }

    /** Returns whether the feature is confirmed present, confirmed absent, or unknown. */
    public AccessibilityStatus getStatus() {
        return status;
    }

    /** Returns optional location or context notes, or null if none. */
    public String getNotes() {
        return notes;
    }

    /** The supported accessibility feature types, per the User Guide's documented list. */
    public enum Type {
        LIFT,
        RAMP,
        SHELTERED_RAMP,
        ACCESSIBLE_WASHROOM,
        STEP_FREE_ENTRANCE,
        REST_POINT,
        AUTOMATIC_DOOR,
        OTHER
    }
}
