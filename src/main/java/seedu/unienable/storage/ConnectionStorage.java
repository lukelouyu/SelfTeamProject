package seedu.unienable.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.exception.StorageException;

/**
 * Loads Connection records from a pipe-delimited connections.txt-format file.
 *
 * <p>Format: {@code CONNECTION|id|from|to|distanceInMetres|accessibility|type|shelter|knownBarrier|notes}.
 * knownBarrier and notes are optional trailing fields. Fields must not contain the '|' delimiter;
 * this is not escaped in v1.0.
 *
 * <p>Loading rejects a non-positive or duplicate ID and a non-positive distance, the same way
 * ActivityStorage rejects the equivalent shapes for activities.txt.
 */
public class ConnectionStorage {
    private static final String CONNECTION_TAG = "CONNECTION";

    /**
     * Loads connections from the given file.
     *
     * @param filePath path to the connections text file
     * @return the loaded connections plus warnings for any skipped malformed lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Connection> load(Path filePath) throws StorageException {
        return load(filePath, null);
    }

    /**
     * Loads connections from the given file, additionally rejecting any connection whose "from"
     * or "to" endpoint does not match a known facility's name (case-insensitively). Without this
     * check, a hand-edited or otherwise corrupted connections.txt could reference a facility that
     * does not exist in facilities.txt at all.
     *
     * @param filePath path to the connections text file
     * @param knownFacilities every facility currently recorded in facilities.txt, used to
     *     validate each loaded connection's endpoints; null skips this cross-file check entirely
     * @return the loaded connections plus warnings for any skipped malformed or invalid lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Connection> load(Path filePath, List<Facility> knownFacilities) throws StorageException {
        Set<String> knownFacilityNames = knownFacilities == null ? null : toLowerNames(knownFacilities);
        List<String> lines = readLines(filePath);
        List<Connection> connections = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                Connection connection = parseLine(line, knownFacilityNames);
                if (!seenIds.add(connection.getId())) {
                    throw new IllegalArgumentException("duplicate connection id " + connection.getId());
                }
                connections.add(connection);
            } catch (IllegalArgumentException e) {
                warnings.add("Line " + (i + 1) + " was skipped: " + e.getMessage());
            }
        }
        return new LoadResult<>(connections, warnings);
    }

    private Set<String> toLowerNames(List<Facility> facilities) {
        Set<String> names = new HashSet<>();
        for (Facility facility : facilities) {
            names.add(facility.getName().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private Connection parseLine(String line, Set<String> knownFacilityNames) {
        String[] fields = line.split("\\|", -1);
        if (!fields[0].equals(CONNECTION_TAG)) {
            throw new IllegalArgumentException("unknown record type \"" + fields[0] + "\"");
        }
        if (fields.length < 8) {
            throw new IllegalArgumentException(
                    "CONNECTION line requires id, from, to, distance, accessibility, type, and shelter");
        }

        int id;
        int distanceInMetres;
        try {
            id = Integer.parseInt(fields[1]);
            distanceInMetres = Integer.parseInt(fields[4]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id and distance must be whole numbers");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("id must be a positive whole number");
        }
        if (distanceInMetres <= 0) {
            throw new IllegalArgumentException("distance must be a positive whole number");
        }

        String from = fields[2];
        String to = fields[3];
        if (knownFacilityNames != null) {
            if (!knownFacilityNames.contains(from.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("from facility \"" + from + "\" is not a known facility");
            }
            if (!knownFacilityNames.contains(to.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("to facility \"" + to + "\" is not a known facility");
            }
        }

        AccessibilityStatus accessibility;
        TraversalType type;
        ShelterStatus shelter;
        try {
            accessibility = AccessibilityStatus.valueOf(fields[5]);
            type = TraversalType.valueOf(fields[6]);
            shelter = ShelterStatus.valueOf(fields[7]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid accessibility, type, or shelter value");
        }

        String knownBarrier = fields.length > 8 && !fields[8].isEmpty() ? fields[8] : null;
        String notes = fields.length > 9 && !fields[9].isEmpty() ? fields[9] : null;

        return new Connection(id, from, to, distanceInMetres, accessibility, type, shelter, knownBarrier, notes);
    }

    private List<String> readLines(Path filePath) throws StorageException {
        try {
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new StorageException("could not read " + filePath, e);
        }
    }
}
