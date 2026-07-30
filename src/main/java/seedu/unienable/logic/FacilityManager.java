package seedu.unienable.logic;

import java.util.List;

import seedu.unienable.accessibility.classes.Facility;
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
}
