package seedu.unienable.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import seedu.unienable.accessibility.classes.Connection;
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
        List<String> lines = readLines(filePath);
        List<Connection> connections = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                connections.add(parseLine(line));
            } catch (IllegalArgumentException e) {
                warnings.add("Line " + (i + 1) + " was skipped: " + e.getMessage());
            }
        }
        return new LoadResult<>(connections, warnings);
    }

    private Connection parseLine(String line) {
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

        return new Connection(id, fields[2], fields[3], distanceInMetres, accessibility, type, shelter,
                knownBarrier, notes);
    }

    private List<String> readLines(Path filePath) throws StorageException {
        try {
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new StorageException("could not read " + filePath, e);
        }
    }
}
