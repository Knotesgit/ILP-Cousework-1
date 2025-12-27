package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.utility.GeoUtilities;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilitiesOnSegmentTest {

    @Test
    @DisplayName("onSegment: point strictly inside segment returns true")
    void onSegment_pointInside_returnsTrue() {
        Coordinate a = new Coordinate(0.0, 0.0);
        Coordinate b = new Coordinate(2.0, 2.0);
        Coordinate p = new Coordinate(1.0, 1.0); // collinear and within

        assertTrue(GeoUtilities.onSegment(p, a, b));
    }

    @Test
    @DisplayName("onSegment: endpoint points are treated as on-segment")
    void onSegment_endpoints_returnTrue() {
        Coordinate a = new Coordinate(0.0, 0.0);
        Coordinate b = new Coordinate(2.0, 2.0);

        assertTrue(GeoUtilities.onSegment(a, a, b));
        assertTrue(GeoUtilities.onSegment(b, a, b));
    }

    @Test
    @DisplayName("onSegment: collinear but outside segment returns false")
    void onSegment_collinearOutside_returnsFalse() {
        Coordinate a = new Coordinate(0.0, 0.0);
        Coordinate b = new Coordinate(2.0, 2.0);
        Coordinate p = new Coordinate(3.0, 3.0); // collinear but beyond endpoint

        assertFalse(GeoUtilities.onSegment(p, a, b));
    }

    @Test
    @DisplayName("onSegment: non-collinear point returns false")
    void onSegment_nonCollinear_returnsFalse() {
        Coordinate a = new Coordinate(0.0, 0.0);
        Coordinate b = new Coordinate(2.0, 2.0);
        Coordinate p = new Coordinate(1.0, 1.1); // not collinear

        assertFalse(GeoUtilities.onSegment(p, a, b));
    }

    @Test
    @DisplayName("onSegment: order of segment endpoints does not matter")
    void onSegment_endpointOrderIrrelevant() {
        Coordinate a = new Coordinate(0.0, 0.0);
        Coordinate b = new Coordinate(2.0, 2.0);
        Coordinate p = new Coordinate(1.0, 1.0);

        assertTrue(GeoUtilities.onSegment(p, a, b));
        assertTrue(GeoUtilities.onSegment(p, b, a));
    }
}
