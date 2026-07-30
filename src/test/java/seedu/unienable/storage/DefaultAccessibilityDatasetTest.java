package seedu.unienable.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;

/**
 * Sanity-checks the bundled default facilities.txt/connections.txt (the NUS FASS Access Route
 * dataset used as sample reference data) parse cleanly and reference each other consistently.
 */
class DefaultAccessibilityDatasetTest {
    private Path resourcePath(String fileName) throws URISyntaxException {
        URL url = getClass().getClassLoader().getResource(fileName);
        return Paths.get(url.toURI());
    }

    @Test
    public void facilitiesTxt_parsesWithoutWarnings() throws Exception {
        LoadResult<Facility> result = new FacilityStorage().load(resourcePath("facilities.txt"));

        assertFalse(result.hasWarnings());
        assertEquals(9, result.getRecords().size());
    }

    @Test
    public void connectionsTxt_parsesWithoutWarnings() throws Exception {
        LoadResult<Connection> result = new ConnectionStorage().load(resourcePath("connections.txt"));

        assertFalse(result.hasWarnings());
        assertEquals(10, result.getRecords().size());
    }

    @Test
    public void connectionsTxt_everyEndpointMatchesAKnownFacilityName() throws Exception {
        LoadResult<Facility> facilities = new FacilityStorage().load(resourcePath("facilities.txt"));
        LoadResult<Connection> connections = new ConnectionStorage().load(resourcePath("connections.txt"));

        Set<String> names = new HashSet<>();
        for (Facility facility : facilities.getRecords()) {
            names.add(facility.getName());
        }
        for (Connection connection : connections.getRecords()) {
            assertTrue(names.contains(connection.getFrom()), connection.getFrom() + " is not a known facility");
            assertTrue(names.contains(connection.getTo()), connection.getTo() + " is not a known facility");
        }
    }
}
