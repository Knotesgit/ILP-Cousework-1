package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Region;
import uk.ac.ed.acp.cw2.service.GeoServiceImpl;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Core unit tests for GeoServiceImpl – focus on geometry logic.
public class GeoServiceTests {

    private GeoServiceImpl geo;

    @BeforeEach
    void setup() { geo = new GeoServiceImpl(); }

    private static Coordinate c(double lng, double lat) {
        Coordinate p = new Coordinate();
        p.setLng(lng); p.setLat(lat);
        return p;
    }

    @Test
    void distanceBetween_shouldBeSymmetricAndPositive() {
        // Distance must be positive and symmetric regardless of argument order.
        Coordinate a = c(-3.192, 55.946);
        Coordinate b = c(-3.192, 55.943);
        double d1 = geo.distanceBetween(a, b);
        double d2 = geo.distanceBetween(b, a);
        assertTrue(d1 > 0);
        assertEquals(d1, d2, 1e-12);
    }

    @Test
    void isNear_withinThreshold_trueOtherwiseFalse() {
        // True if distance < 0.00015; false when exactly at the threshold.
        Coordinate a = c(0, 0);
        Coordinate b = c(0.00014, 0);
        Coordinate c = c(0.00015, 0);
        assertTrue(geo.isNear(a, b));
        assertFalse(geo.isNear(a, c));
    }

    @Test
    void isValidAngle_onlyMultiplesOf22point5() {
        // Valid only for multiples of 22.5° (also must >= 0).
        assertTrue(geo.isValidAngle(45.0));
        assertFalse(geo.isValidAngle(13.0));
        assertFalse(geo.isValidAngle(-10.5));
    }

    @Test
    void nextPosition_movesExpectedDistance() {
        // Moving north (90°) should change latitude by STEP only.
        Coordinate start = c(-3.0, 55.0);
        Coordinate next = geo.nextPosition(start, 90.0);
        assertEquals(start.getLng(), next.getLng(), 1e-12);
        assertEquals(start.getLat() + 0.00015, next.getLat(), 1e-12);
    }

    @Test
    void onSegment_returnsTrueOnlyForPointsOnSegment() {
        Coordinate p = c(0, 0);
        Coordinate q = c(2, 0);
        assertTrue(geo.onSegment(c(1, 0), p, q));
        assertTrue(geo.onSegment(c(0, 0), p, q));
        assertTrue(geo.onSegment(c(2, 0), p, q));
        assertFalse(geo.onSegment(c(1.0, 1e-12), p, q));
        assertFalse(geo.onSegment(c(3, 0), p, q));
        assertFalse(geo.onSegment(c(-1, 0), p, q));
        assertFalse(geo.onSegment(c(1, 0.1), p, q));
    }

    @Test
    void isPointInRegion_inclusiveOfBorder() {
        List<Coordinate> square = Arrays.asList(
                c(0,1), c(0,0), c(1,0), c(1,1), c(0,1)  // closed
        );
        Region region = new Region();
        region.setVertices(square);
        assertTrue(geo.isPointInRegion(c(0.5,0.5), square)); // inside
        assertTrue(geo.isPointInRegion(c(0,0.5), square));   // border
        assertFalse(geo.isPointInRegion(c(2,0.5), square));  // outside
    }
}

