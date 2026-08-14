package seedu.unienable.ui.accessibility;

/**
 * Shared disclaimer appended to every facility/connection command's output. The bundled dataset
 * is digitised from a real campus accessibility map, but distances were estimated from the map's
 * grid (it has no printed scale) rather than measured, so this must not be read as verified or
 * real-time navigation guidance.
 *
 * <p>Lives in {@code ui.accessibility} rather than {@code command} - it is presentation content
 * (text appended to formatted output), and {@link RouteFormatter} (also in {@code ui.accessibility}
 * ) needs it too; placing it in {@code command} would force {@code ui} to depend on {@code command}
 * to reach it, contradicting this project's general {@code command -> ui} presentation-flow
 * direction.
 */
public final class AccessibilityDisclaimer {
    public static final String TEXT = "Sample local accessibility reference data. Distances are "
            + "estimates and may be incomplete. Please verify with current campus information when needed.";

    private AccessibilityDisclaimer() {
    }
}
