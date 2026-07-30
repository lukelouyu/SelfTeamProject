package seedu.unienable.logic;

import java.util.ArrayList;
import java.util.List;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.classes.FacilityFeature;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.exception.InvalidIndexException;

/**
 * Holds the loaded facility reference dataset in memory. Read-only: no in-app command adds,
 * edits, or deletes facility records.
 */
public class FacilityManager {
    private final List<Facility> facilities;

    public FacilityManager(List<Facility> facilities) {
        this.facilities = List.copyOf(facilities);
    }

    /** Returns every known facility, in load order. */
    public List<Facility> list() {
        return facilities;
    }

    /**
     * Finds a facility by name, case-insensitively.
     *
     * @param name the facility name to look up
     * @return the matching facility
     * @throws InvalidIndexException if no facility has that name
     */
    public Facility findByName(String name) throws InvalidIndexException {
        for (Facility facility : facilities) {
            if (facility.getName().equalsIgnoreCase(name)) {
                return facility;
            }
        }
        throw new InvalidIndexException("Facility \"" + name + "\" is not recognised.");
    }

    /**
     * Finds every facility with at least one feature of the given type at the given status.
     *
     * @param type the feature type to match
     * @param status the feature status to match
     * @return the matching facilities, in load order
     */
    public List<Facility> findByFeature(FacilityFeature.Type type, AccessibilityStatus status) {
        List<Facility> result = new ArrayList<>();
        for (Facility facility : facilities) {
            if (hasFeature(facility, type, status)) {
                result.add(facility);
            }
        }
        return result;
    }

    private boolean hasFeature(Facility facility, FacilityFeature.Type type, AccessibilityStatus status) {
        for (FacilityFeature feature : facility.getFeatures()) {
            if (feature.getType() == type && feature.getStatus() == status) {
                return true;
            }
        }
        return false;
    }
}
