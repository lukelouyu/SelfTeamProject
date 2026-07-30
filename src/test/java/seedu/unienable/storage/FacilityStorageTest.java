package seedu.unienable.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.classes.FacilityFeature;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.exception.StorageException;

class FacilityStorageTest {
    @TempDir
    Path tempDir;

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("facilities.txt");
        Files.write(file, List.of(lines));
        return file;
    }

    @Test
    public void load_validFile_parsesFacilitiesAndFeatures() throws Exception {
        Path file = writeFile(
                "FACILITY|F01|COM3|Engineering building",
                "FEATURE|F01|LIFT|YES|Level 1 lobby",
                "FEATURE|F01|ACCESSIBLE_WASHROOM|YES|Level 2");

        LoadResult<Facility> result = new FacilityStorage().load(file);

        assertEquals(0, result.getWarnings().size());
        assertEquals(1, result.getRecords().size());
        Facility facility = result.getRecords().get(0);
        assertEquals("F01", facility.getId());
        assertEquals("COM3", facility.getName());
        assertEquals("Engineering building", facility.getDescription());
        assertEquals(2, facility.getFeatures().size());
        assertEquals(FacilityFeature.Type.LIFT, facility.getFeatures().get(0).getType());
        assertEquals(AccessibilityStatus.YES, facility.getFeatures().get(0).getStatus());
    }

    @Test
    public void load_facilityWithoutDescriptionOrFeatureNotes_treatsAsNull() throws Exception {
        Path file = writeFile(
                "FACILITY|F02|AS6",
                "FEATURE|F02|REST_POINT|NO");

        LoadResult<Facility> result = new FacilityStorage().load(file);

        Facility facility = result.getRecords().get(0);
        assertNull(facility.getDescription());
        assertNull(facility.getFeatures().get(0).getNotes());
    }

    @Test
    public void load_unknownRecordTag_recordsWarningAndSkipsLine() throws Exception {
        Path file = writeFile("BOGUS|F01|COM3");

        LoadResult<Facility> result = new FacilityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("Line 1"));
    }

    @Test
    public void load_featureReferencingUnknownFacility_recordsWarning() throws Exception {
        Path file = writeFile("FEATURE|F99|LIFT|YES|Somewhere");

        LoadResult<Facility> result = new FacilityStorage().load(file);

        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("unknown facility ID F99"));
    }

    @Test
    public void load_invalidFeatureTypeOrStatus_recordsWarningAndKeepsOtherLines() throws Exception {
        Path file = writeFile(
                "FACILITY|F01|COM3",
                "FEATURE|F01|NOT_A_TYPE|YES|Somewhere",
                "FEATURE|F01|LIFT|MAYBE|Somewhere");

        LoadResult<Facility> result = new FacilityStorage().load(file);

        assertEquals(2, result.getWarnings().size());
        assertEquals(1, result.getRecords().size());
        assertEquals(0, result.getRecords().get(0).getFeatures().size());
    }

    @Test
    public void load_missingFile_throwsStorageException() {
        Path missing = tempDir.resolve("does-not-exist.txt");

        assertThrows(StorageException.class, () -> new FacilityStorage().load(missing));
    }
}
