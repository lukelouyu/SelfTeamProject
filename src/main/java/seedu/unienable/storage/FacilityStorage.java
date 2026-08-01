package seedu.unienable.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.classes.FacilityFeature;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.exception.StorageException;

/**
 * Loads Facility records from a pipe-delimited facilities.txt-format file.
 *
 * <p>Format: {@code FACILITY|id|name|description} followed by zero or more
 * {@code FEATURE|facilityId|type|status|notes} lines. A FACILITY line must appear before any FEATURE
 * line that references it. Fields must not contain the '|' delimiter; this is not escaped in v1.0.
 *
 * <p>Loading rejects a duplicate facility ID and a duplicate facility name (case-insensitively) -
 * facilities are looked up by name (see FacilityManager.findByName), so two facilities sharing a
 * name would make that lookup ambiguous - as well as a blank facility/feature ID or name, and a
 * record with more fields than its format defines.
 */
public class FacilityStorage {
    private static final String FACILITY_TAG = "FACILITY";
    private static final String FEATURE_TAG = "FEATURE";
    private static final int FACILITY_MIN_FIELDS = 3;
    private static final int FACILITY_MAX_FIELDS = 4;
    private static final int FEATURE_MIN_FIELDS = 4;
    private static final int FEATURE_MAX_FIELDS = 5;

    /**
     * Loads facilities from the given file.
     *
     * @param filePath path to the facilities text file
     * @return the loaded facilities plus warnings for any skipped malformed lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Facility> load(Path filePath) throws StorageException {
        List<String> lines = readLines(filePath);
        Map<String, PendingFacility> pending = new LinkedHashMap<>();
        Set<String> seenNames = new HashSet<>();
        List<String> warnings = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                parseLine(line, pending, seenNames);
            } catch (IllegalArgumentException e) {
                warnings.add("Line " + (i + 1) + " was skipped: " + e.getMessage());
            }
        }

        List<Facility> facilities = new ArrayList<>();
        for (Map.Entry<String, PendingFacility> entry : pending.entrySet()) {
            PendingFacility facility = entry.getValue();
            facilities.add(new Facility(entry.getKey(), facility.name, facility.description, facility.features));
        }
        return new LoadResult<>(facilities, warnings);
    }

    private void parseLine(String line, Map<String, PendingFacility> pending, Set<String> seenNames) {
        String[] fields = line.split("\\|", -1);
        switch (fields[0]) {
        case FACILITY_TAG:
            parseFacilityLine(fields, pending, seenNames);
            break;
        case FEATURE_TAG:
            parseFeatureLine(fields, pending);
            break;
        default:
            throw new IllegalArgumentException("unknown record type \"" + fields[0] + "\"");
        }
    }

    private void parseFacilityLine(String[] fields, Map<String, PendingFacility> pending, Set<String> seenNames) {
        if (fields.length < FACILITY_MIN_FIELDS || fields.length > FACILITY_MAX_FIELDS) {
            throw new IllegalArgumentException("FACILITY line requires an id and a name, and no more than "
                    + "an optional description after that");
        }
        String id = fields[1];
        String name = fields[2];
        if (id.isBlank()) {
            throw new IllegalArgumentException("facility id must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("facility name must not be blank");
        }
        if (pending.containsKey(id)) {
            throw new IllegalArgumentException("duplicate facility id " + id);
        }
        if (!seenNames.add(name.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("duplicate facility name \"" + name + "\"");
        }
        String description = fields.length > 3 && !fields[3].isEmpty() ? fields[3] : null;
        pending.put(id, new PendingFacility(name, description));
    }

    private void parseFeatureLine(String[] fields, Map<String, PendingFacility> pending) {
        if (fields.length < FEATURE_MIN_FIELDS || fields.length > FEATURE_MAX_FIELDS) {
            throw new IllegalArgumentException("FEATURE line requires facility ID, type, and status, and no more "
                    + "than an optional notes field after that");
        }
        String facilityId = fields[1];
        if (facilityId.isBlank()) {
            throw new IllegalArgumentException("FEATURE facility ID must not be blank");
        }
        PendingFacility facility = pending.get(facilityId);
        if (facility == null) {
            throw new IllegalArgumentException("FEATURE references unknown facility ID " + facilityId);
        }
        FacilityFeature.Type type;
        AccessibilityStatus status;
        try {
            type = FacilityFeature.Type.valueOf(fields[2]);
            status = AccessibilityStatus.valueOf(fields[3]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid feature type or status");
        }
        String notes = fields.length > 4 && !fields[4].isEmpty() ? fields[4] : null;
        facility.features.add(new FacilityFeature(type, status, notes));
    }

    private List<String> readLines(Path filePath) throws StorageException {
        try {
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new StorageException("could not read " + filePath, e);
        }
    }

    private static final class PendingFacility {
        private final String name;
        private final String description;
        private final List<FacilityFeature> features = new ArrayList<>();

        private PendingFacility(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
}
