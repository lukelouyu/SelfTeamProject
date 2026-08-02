package seedu.unienable.ui.preference;

import java.util.ArrayList;
import java.util.List;

import seedu.unienable.model.preference.PreferenceProfile;

/** Formats global planning preferences and atomic old/new previews as plain text. */
public final class PreferenceFormatter {
    private PreferenceFormatter() {
    }

    /**
     * Formats all four active values and their global scope.
     *
     * @param profile the profile to format
     * @return deterministic multiline profile text
     */
    public static String formatProfile(PreferenceProfile profile) {
        return "Preference profile\n\n"
                + "Preferred daily start: " + profile.getPreferredStart() + "\n"
                + "Preferred daily end: " + profile.getPreferredEnd() + "\n"
                + "Minimum buffer: " + profile.getMinimumBufferMinutes() + " minutes\n"
                + "Tomato suggestion: " + profile.getTomatoSuggestion() + "\n\n"
                + "These preferences apply to every day.";
    }

    /**
     * Formats only changed fields in stable profile order.
     *
     * @param oldProfile the current profile
     * @param newProfile the proposed profile
     * @return one line per changed field, or an empty string if none changed
     */
    public static String formatChanges(PreferenceProfile oldProfile,
            PreferenceProfile newProfile) {
        List<String> changes = new ArrayList<>();
        addChange(changes, "Preferred daily start", oldProfile.getPreferredStart(),
                newProfile.getPreferredStart());
        addChange(changes, "Preferred daily end", oldProfile.getPreferredEnd(),
                newProfile.getPreferredEnd());
        addChange(changes, "Minimum buffer", oldProfile.getMinimumBufferMinutes() + " minutes",
                newProfile.getMinimumBufferMinutes() + " minutes");
        addChange(changes, "Tomato suggestion", oldProfile.getTomatoSuggestion(),
                newProfile.getTomatoSuggestion());
        return String.join("\n", changes);
    }

    private static void addChange(List<String> changes, String label, Object oldValue,
            Object newValue) {
        if (!oldValue.equals(newValue)) {
            changes.add(label + " | Old: " + oldValue + " | New: " + newValue);
        }
    }
}
