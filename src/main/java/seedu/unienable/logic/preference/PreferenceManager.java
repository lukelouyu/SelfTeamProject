package seedu.unienable.logic.preference;

import seedu.unienable.model.preference.PreferenceProfile;

/** Owns the one active global planning preference profile. */
public class PreferenceManager {
    private PreferenceProfile profile = PreferenceProfile.defaults();

    /**
     * Returns the active immutable profile.
     *
     * @return the active profile
     */
    public PreferenceProfile getProfile() {
        return profile;
    }

    /**
     * Replaces the active profile with an already validated immutable value.
     *
     * @param profile the non-null replacement profile
     * @throws IllegalArgumentException if profile is null
     */
    public void setProfile(PreferenceProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("preference profile must not be null");
        }
        this.profile = profile;
    }

    /** Restores the single documented default profile. */
    public void reset() {
        profile = PreferenceProfile.defaults();
    }

    /**
     * Returns whether the active profile already equals every documented default.
     *
     * @return true exactly when the profile is the documented default
     */
    public boolean isDefault() {
        return PreferenceProfile.defaults().equals(profile);
    }
}
