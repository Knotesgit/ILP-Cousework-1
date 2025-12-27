package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.utility.GeoUtilities;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilitiesDistanceBetweenTest {

    /**
     * Contract under test (FR-U1):
     * distanceBetween(a,b) computes correct Euclidean distance in degree-space.
     */
    private static final double EPS = 1e-12;

    static Stream<Arguments> pythagoreanCases() {
        return Stream.of(
                Arguments.of(
                        new Coordinate(0.0, 0.0),
                        new Coordinate(0.0, 0.0)
                ),
                Arguments.of(
                        new Coordinate(1.0, 2.0),
                        new Coordinate(1.0, 2.0)
                ),
                Arguments.of(
                        new Coordinate(0.0, 0.0),
                        new Coordinate(0.00012, 0.00009)
                ),
                Arguments.of(
                        new Coordinate(-3.1883, 55.9533),
                        new Coordinate(-3.1883 + 0.00012, 55.9533 + 0.00009)
                ),
                Arguments.of(
                        new Coordinate(0.0, 0.0),
                        new Coordinate(0.00015, 0.0)
                ),
                Arguments.of(
                        new Coordinate(0.0, 0.0),
                        new Coordinate(0.0, -0.00015)
                )
        );
    }

    @Test
    @DisplayName("FR-U1: identity: distanceBetween(p,p) == 0")
    void distance_identityIsZero() {
        Coordinate p = new Coordinate(-3.1883, 55.9533);
        assertEquals(0.0, GeoUtilities.distanceBetween(p, p), 0.0);
    }

    @Test
    @DisplayName("FR-U1: symmetry: distanceBetween(a,b) == distanceBetween(b,a)")
    void distance_isSymmetric() {
        Coordinate a = new Coordinate(-3.19, 55.95);
        Coordinate b = new Coordinate(-3.18, 55.96);

        double dab = GeoUtilities.distanceBetween(a, b);
        double dba = GeoUtilities.distanceBetween(b, a);

        assertEquals(dab, dba, EPS);
    }

    @ParameterizedTest(name = "a={0}, b={1}")
    @MethodSource("pythagoreanCases")
    @DisplayName("FR-U1: matches independent oracle sqrt(dx^2 + dy^2)")
    void distance_matchesIndependentOracle(Coordinate a, Coordinate b) {
        double dx = b.getLng() - a.getLng();
        double dy = b.getLat() - a.getLat();

        // Independent oracle (no reliance on other GeoUtilities methods)
        double expected = Math.hypot(dx, dy);
        double actual = GeoUtilities.distanceBetween(a, b);

        assertEquals(expected, actual, EPS);
        assertTrue(actual >= 0.0, "Distance must be non-negative");
    }

    @Test
    @DisplayName("FR-U1: axis-aligned distance equals absolute delta on that axis")
    void distance_axisAligned() {
        Coordinate a = new Coordinate(10.0, 20.0);
        Coordinate b = new Coordinate(10.00015, 20.0); // dx only

        double d = GeoUtilities.distanceBetween(a, b);
        assertEquals(0.00015, d, EPS);
    }

    @Test
    @DisplayName("FR-U1 contract boundary: null inputs throw (documented behaviour)")
    void distance_nullInputs_throw() {
        Coordinate a = new Coordinate(0.0, 0.0);

        assertThrows(NullPointerException.class, () -> GeoUtilities.distanceBetween(null, a));
        assertThrows(NullPointerException.class, () -> GeoUtilities.distanceBetween(a, null));
        assertThrows(NullPointerException.class, () -> GeoUtilities.distanceBetween(null, null));
    }
}

