package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.utility.GeoUtilities;

import java.util.stream.DoubleStream;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilitiesNextPositionTest {

    /**
     * Contract under test (FR-U2):
     * For any valid angle (multiple of 22.5°), nextPosition(start, angle) returns a point
     * exactly one STEP away from start, in the corresponding direction.
     */
    private static final double STEP = 0.00015;
    private static final double EPS  = 1e-10;

    static DoubleStream validAngles() {
        // 0, 22.5, ..., 337.5 (16 directions)
        return DoubleStream.iterate(0.0, a -> a + 22.5).limit(16);
    }

    @ParameterizedTest(name = "angle={0}")
    @MethodSource("validAngles")
    @DisplayName("FR-U2-1: nextPosition returns a point exactly one STEP away (independent oracle)")
    void nextPosition_hasExactStepLength(double angle) {
        Coordinate start = new Coordinate(-3.1883, 55.9533);
        Coordinate next = GeoUtilities.nextPosition(start, angle);

        assertNotNull(next, "nextPosition must return a Coordinate");
        assertNotNull(next.getLng(), "lng must not be null");
        assertNotNull(next.getLat(), "lat must not be null");

        double dx = next.getLng() - start.getLng();
        double dy = next.getLat() - start.getLat();

        // Independent oracle: no reliance on other GeoUtilities methods
        double dist = Math.hypot(dx, dy);
        assertEquals(STEP, dist, EPS, "Returned coordinate must be exactly one STEP away");
    }

    @ParameterizedTest(name = "angle={0}")
    @MethodSource("validAngles")
    @DisplayName("FR-U2-2: displacement components match STEP*cos/sin(angle)")
    void nextPosition_matchesTrigonometricDisplacement(double angle) {
        Coordinate start = new Coordinate(1.2345, -6.7890);
        Coordinate next = GeoUtilities.nextPosition(start, angle);

        double rad = Math.toRadians(angle);
        double expectedDx = STEP * Math.cos(rad);
        double expectedDy = STEP * Math.sin(rad);

        double actualDx = next.getLng() - start.getLng();
        double actualDy = next.getLat() - start.getLat();

        assertEquals(expectedDx, actualDx, EPS, "Δlng must equal STEP*cos(angle)");
        assertEquals(expectedDy, actualDy, EPS, "Δlat must equal STEP*sin(angle)");
    }

    @ParameterizedTest(name = "angle={0}")
    @MethodSource("validAngles")
    @DisplayName("FR-U2-3: deterministic output for same inputs")
    void nextPosition_isDeterministic(double angle) {
        Coordinate start = new Coordinate(-3.2, 55.9);

        Coordinate a = GeoUtilities.nextPosition(start, angle);
        Coordinate b = GeoUtilities.nextPosition(start, angle);

        assertEquals(a.getLng(), b.getLng(), EPS, "lng must be deterministic");
        assertEquals(a.getLat(), b.getLat(), EPS, "lat must be deterministic");
    }

    @ParameterizedTest(name = "angle={0}")
    @MethodSource("validAngles")
    @DisplayName("FR-U2-4: start coordinate is not mutated")
    void nextPosition_doesNotMutateStart(double angle) {
        Coordinate start = new Coordinate(10.0, 20.0);
        double lng0 = start.getLng();
        double lat0 = start.getLat();

        GeoUtilities.nextPosition(start, angle);

        assertEquals(lng0, start.getLng(), 0.0, "start.lng must not change");
        assertEquals(lat0, start.getLat(), 0.0, "start.lat must not change");
    }

    @Test
    @DisplayName("FR-U2-5 contract boundary: null start throws (documented behaviour)")
    void nextPosition_nullStart_throws() {
        assertThrows(NullPointerException.class, () -> GeoUtilities.nextPosition(null, 0.0));
    }
}
