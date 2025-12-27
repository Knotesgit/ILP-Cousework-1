package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.service.GeoServiceImpl;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Core unit tests for GeoServiceImpl – geometry logic only.
 * Spec basis:
 * - Distances are Euclidean in degrees (planar).
 * - “Close” means strictly < 0.00015°.
 * - Each horizontal move is 0.00015° with ±1e-12 tolerance; angles are multiples of 22.5° with 0=E,90=N.
 * - Point-in-region includes border; region must be closed (controller rejects open regions).
 */
public class GeoServiceTests {

    private static final double STEP = 0.00015;
    private static final double EPS  = 1e-12;

    private GeoServiceImpl geo;

    @BeforeEach
    void setup() { geo = new GeoServiceImpl(); }

    private static Coordinate c(double lng, double lat) {
        Coordinate p = new Coordinate();
        p.setLng(lng); p.setLat(lat);
        return p;
    }

    // distanceBetween
    @Test
    void distanceBetween_zeroForSamePoint_andSymmetricAndNonNegative() {
        Coordinate a = c(-3.192473, 55.946233);
        assertEquals(0.0, geo.distanceBetween(a, a), EPS);
        Coordinate b = c(-3.184319, 55.942617);
        double d1 = geo.distanceBetween(a, b);
        double d2 = geo.distanceBetween(b, a);
        assertTrue(d1 >= 0.0);
        assertEquals(d1, d2, EPS);
    }

    // isNear (strictly < 0.00015°)
    @Test
    void isNear_trueJustBelowThreshold_falseAtThreshold() {
        Coordinate a = c(0, 0);
        Coordinate justBelow = c(STEP - 1e-11, 0);
        Coordinate atThreshold = c(STEP, 0);
        assertTrue(geo.isNear(a, justBelow));
        assertFalse(geo.isNear(a, atThreshold));
    }

    // nextPosition (step = 0.00015° in compass directions)
    @Test
    void nextPosition_movesCorrectStep_inCardinalAndDiagonalDirections() {
        Coordinate s = c(10.0, 20.0);

        // East (0°): +lng
        Coordinate e0 = geo.nextPosition(s, 0.0);
        assertEquals(10.0 + STEP, e0.getLng(), EPS);
        assertEquals(20.0,        e0.getLat(), EPS);

        // North (90°): +lat
        Coordinate n90 = geo.nextPosition(s, 90.0);
        assertEquals(10.0,        n90.getLng(), EPS);
        assertEquals(20.0 + STEP, n90.getLat(), EPS);

        // West (180°): -lng
        Coordinate w180 = geo.nextPosition(s, 180.0);
        assertEquals(10.0 - STEP, w180.getLng(), EPS);
        assertEquals(20.0,        w180.getLat(), EPS);

        // 45°: +lng,+lat equally
        double diag = STEP / Math.sqrt(2.0);
        Coordinate ne45 = geo.nextPosition(s, 45.0);
        assertEquals(10.0 + diag, ne45.getLng(), EPS);
        assertEquals(20.0 + diag, ne45.getLat(), EPS);
    }

    // onSegment (inclusive of endpoints; handles vertical/diagonal)
    @Test
    void onSegment_inclusiveEndpoints_andHandlesVerticalAndDiagonal() {
        // Vertical
        Coordinate p = c(2, 1), q = c(2, 3);
        assertTrue(geo.onSegment(c(2, 2), p, q));
        assertFalse(geo.onSegment(c(2.0001, 2), p, q));

        // Diagonal
        Coordinate a = c(0, 0), b = c(2, 2);
        assertTrue(geo.onSegment(c(1, 1), a, b));
        assertFalse(geo.onSegment(c(1, 1.0001), a, b));
    }

    // isPointInRegion (border inclusive; ray-casting edge cases)
    @Test
    void isPointInRegion_inclusiveOfBorder_andTypicalInsideOutside() {
        List<Coordinate> rect = Arrays.asList(c(0,0),
                c(2,0),
                c(2,1),
                c(0,1),
                c(0,0)
                );
        assertTrue(geo.isPointInRegion(c(1,0.5), rect)); // inside
        assertTrue(geo.isPointInRegion(c(1,0), rect));   // on horizontal edge (border is inside)
        assertTrue(geo.isPointInRegion(c(0,0), rect));   // vertex (border is inside)
        assertFalse(geo.isPointInRegion(c(3,0.5), rect));// outside
    }

    @Test
    void isPointInRegion_concavePolygon_holeLikeAreaHandled() {
        // Concave Γ-shape (closed)
        List<Coordinate> poly = Arrays.asList(
                c(0,0),
                c(3,0),
                c(3,1),
                c(1,1),
                c(1,3),
                c(0,3),
                c(0,0)
                );
        assertTrue(geo.isPointInRegion(c(0.5,0.5), poly)); // inside solid
        assertFalse(geo.isPointInRegion(c(2,2), poly));    // outside (in the "gap")
    }
}

