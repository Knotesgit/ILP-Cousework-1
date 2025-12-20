package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Drone;
import uk.ac.ed.acp.cw2.data.QueryCondition;
import uk.ac.ed.acp.cw2.utility.QueryDroneHelper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class QueryDroneHelperMatchesConditionsTests {

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

    // Helper to create a QueryCondition
    private QueryCondition cond(String attr, String op, String value) {
        QueryCondition c = new QueryCondition();
        c.setAttribute(attr);
        c.setOperator(op);
        c.setValue(value);
        return c;
    }

    // ---------- Empty condition list ----------

    @Test
    void emptyConditions_returnsTrue() {
        // AND over an empty set is considered true
        boolean result = QueryDroneHelper.matchesConditions(d, List.of());
        assertTrue(result);
    }

    // ---------- "=" operator ----------

    @Test
    void singleEquals_conditionTrue_returnsTrue() {
        // id = "D1" is true for the fixture drone
        var conditions = List.of(
                cond("id", "=", "D1")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertTrue(result);
    }

    @Test
    void singleEquals_conditionFalse_returnsFalse() {
        // id = "D2" is false
        var conditions = List.of(
                cond("id", "=", "D2")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertFalse(result);
    }

    // ---------- "!=" operator ----------

    @Test
    void notEquals_conditionTrue_returnsTrue() {
        // id != "D2" is true
        var conditions = List.of(
                cond("id", "!=", "D2")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertTrue(result);
    }

    @Test
    void notEquals_conditionFalse_returnsFalse() {
        // id != "D1" is false
        var conditions = List.of(
                cond("id", "!=", "D1")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertFalse(result);
    }

    // ---------- "<" and ">" numeric operators (via compareNumeric) ----------

    @Test
    void lessThan_conditionTrue_returnsTrue() {
        // capacity = 10.0, so capacity < 20.0 is true
        var conditions = List.of(
                cond("capacity", "<", "20.0")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertTrue(result);
    }

    @Test
    void lessThan_conditionFalse_returnsFalse() {
        // capacity = 10.0, so capacity < 5.0 is false
        var conditions = List.of(
                cond("capacity", "<", "5.0")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertFalse(result);
    }

    @Test
    void greaterThan_conditionTrue_returnsTrue() {
        // maxMoves = 200, so maxMoves > 100 is true
        var conditions = List.of(
                cond("maxmoves", ">", "100")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertTrue(result);
    }

    @Test
    void greaterThan_conditionFalse_returnsFalse() {
        // maxMoves = 200, so maxMoves > 500 is false
        var conditions = List.of(
                cond("maxmoves", ">", "500")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertFalse(result);
    }

    @Test
    void numericCompare_invalidNumber_returnsFalse() {
        // "abc" cannot be parsed as a number, compareNumeric returns false
        var conditions = List.of(
                cond("capacity", "<", "abc")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertFalse(result);
    }

    // ---------- Invalid attribute / operator ----------

    @Test
    void unknownAttributeWithNumericOp_returnsFalse() {
        // compareNumeric sees unknown attribute, gets NaN, returns false
        var conditions = List.of(
                cond("unknownattr", "<", "10.0")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertFalse(result);
    }

    @Test
    void invalidOperator_returnsFalseImmediately() {
        // any operator not in { "=", "!=", "<", ">" } should cause false
        var conditions = List.of(
                cond("id", "==", "D1")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertFalse(result);
    }

    // ---------- Multiple conditions (AND behavior) ----------

    @Test
    void multipleConditions_allTrue_returnsTrue() {
        // id = "D1" AND capacity > 5.0 AND maxMoves < 300
        var conditions = List.of(
                cond("id", "=", "D1"),
                cond("capacity", ">", "5.0"),
                cond("maxmoves", "<", "300")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertTrue(result);
    }

    @Test
    void multipleConditions_oneFalse_returnsFalse() {
        // id = "D1" (true) AND capacity < 5.0 (false)
        var conditions = List.of(
                cond("id", "=", "D1"),
                cond("capacity", "<", "5.0")
        );
        boolean result = QueryDroneHelper.matchesConditions(d, conditions);
        assertFalse(result);
    }
}

