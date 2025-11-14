package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Drone;
import uk.ac.ed.acp.cw2.utility.QueryDroneHelper;

import static org.junit.jupiter.api.Assertions.*;

class QueryDroneHelperMatchesTests {

    private Drone d;

    @BeforeEach
    void setup() {
        d = new Drone();
        d.setId("D1");
        d.setName("CargoAlpha");

        Drone.DroneCapability cap = new Drone.DroneCapability();
        cap.setCooling(true);
        cap.setHeating(false);
        cap.setCapacity(10.0);
        cap.setMaxMoves(200);
        cap.setCostPerMove(1.5);
        cap.setCostInitial(2.0);
        cap.setCostFinal(3.0);

        d.setCapability(cap);
    }

    // ---------- String fields (MC/DC true + false + case-insensitive) ----------

    @Test
    void id_equal_returnsTrue() {
        assertTrue(QueryDroneHelper.matches(d, "id", "D1"));
    }

    @Test
    void id_notEqual_returnsFalse() {
        assertFalse(QueryDroneHelper.matches(d, "id", "D2"));
    }

    @Test
    void name_caseInsensitiveMatch_returnsTrue() {
        assertTrue(QueryDroneHelper.matches(d, "NAME", "cargoalpha"));
    }

    // ---------- Boolean fields (true/false + weird input) ----------

    @Test
    void cooling_trueMatch_returnsTrue() {
        assertTrue(QueryDroneHelper.matches(d, "cooling", "true"));
    }

    @Test
    void cooling_falseMismatch_returnsFalse() {
        assertFalse(QueryDroneHelper.matches(d, "cooling", "false"));
    }

    @Test
    void heating_weirdStringParsesFalse_returnsFalse() {
        assertFalse(QueryDroneHelper.matches(d, "heating", "Yeah"));
    }

    // ---------- Numeric fields (equal, not equal, parse error path) ----------

    @Test
    void capacity_equal_returnsTrue() {
        assertTrue(QueryDroneHelper.matches(d, "capacity", "10.0"));
    }

    @Test
    void capacity_notEqual_returnsFalse() {
        assertFalse(QueryDroneHelper.matches(d, "capacity", "5.0"));
    }

    @Test
    void costInitial_parseError_returnsFalse() {
        assertFalse(QueryDroneHelper.matches(d, "costinitial", "abc"));
    }

    // ---------- Default branch ----------

    @Test
    void unknownAttribute_returnsFalse() {
        assertFalse(QueryDroneHelper.matches(d, "unknown", "value"));
    }
}
