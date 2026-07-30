package seedu.unienable.accessibility.classes;

import java.util.Collections;
import java.util.List;

/** A known facility and its recorded accessibility features. Read-only reference data. */
public class Facility {
    private final String id;
    private final String name;
    private final String description;
    private final List<FacilityFeature> features;

    /**
     * Creates a Facility.
     *
     * @param id stable facility ID, e.g. "F01"
     * @param name facility name, e.g. "COM3"
     * @param description optional facility description, or null if none
     * @param features the facility's recorded accessibility features
     */
    public Facility(String id, String name, String description, List<FacilityFeature> features) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.features = Collections.unmodifiableList(features);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<FacilityFeature> getFeatures() {
        return features;
    }
}
