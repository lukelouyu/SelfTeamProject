package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.exception.InvalidIndexException;

class FacilityManagerTest {
    @Test
    public void list_returnsFacilitiesInLoadOrder() {
        Facility com3 = new Facility("F05", "COM3", null, List.of());
        Facility com1 = new Facility("F04", "COM1", null, List.of());
        FacilityManager manager = new FacilityManager(List.of(com3, com1));

        assertEquals(List.of(com3, com1), manager.list());
    }

    @Test
    public void findByName_isCaseInsensitive() throws Exception {
        Facility com3 = new Facility("F05", "COM3", null, List.of());
        FacilityManager manager = new FacilityManager(List.of(com3));

        assertEquals(com3, manager.findByName("com3"));
    }

    @Test
    public void findByName_unknownName_throwsInvalidIndexException() {
        FacilityManager manager = new FacilityManager(List.of());

        assertThrows(InvalidIndexException.class, () -> manager.findByName("COM3"));
    }
}
