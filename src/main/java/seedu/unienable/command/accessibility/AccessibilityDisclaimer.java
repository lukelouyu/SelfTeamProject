package seedu.unienable.command.accessibility;

/**
 * Shared disclaimer appended to every facility/connection command's output. The bundled dataset
 * is digitised from a real campus accessibility map, but distances were estimated from the map's
 * grid (it has no printed scale) rather than measured, so this must not be read as verified or
 * real-time navigation guidance.
 */
public final class AccessibilityDisclaimer {
    public static final String TEXT = "Sample local accessibility reference data. Distances are "
            + "estimates and may be incomplete. Please verify with current campus information when needed.";

    private AccessibilityDisclaimer() {
    }
}
