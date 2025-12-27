package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.utility.GeoUtilities;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilitiesIsPointInRegionTest {

    /**
     * Contract under test (FR-U3):
     * isPointInRegion(p, polygonVertices) must treat boundary points (on an edge or a vertex)
     * as inside the polygon.
     *
     * IMPORTANT: GeoUtilities.isPointInRegion assumes the polygon is CLOSED:
     * last vertex == first vertex.
     */
    private static List<Coordinate> unitSquareClosed() {
        // Square: (0,0) -> (1,0) -> (1,1) -> (0,1) -> (0,0)
        return List.of(
                new Coordinate(0.0, 0.0),
                new Coordinate(1.0, 0.0),
                new Coordinate(1.0, 1.0),
                new Coordinate(0.0, 1.0),
                new Coordinate(0.0, 0.0)
        );
    }

    @Test
    @DisplayName("FR-U3: point strictly inside polygon returns true")
    void pointInside_returnsTrue() {
        var poly = unitSquareClosed();
        var p = new Coordinate(0.5, 0.5);

        assertTrue(GeoUtilities.isPointInRegion(p, poly));
    }

    @Test
    @DisplayName("FR-U3: point strictly outside polygon returns false")
    void pointOutside_returnsFalse() {
        var poly = unitSquareClosed();
        var p = new Coordinate(1.5, 0.5);

        assertFalse(GeoUtilities.isPointInRegion(p, poly));
    }

    @Test
    @DisplayName("FR-U3: point on an edge is treated as inside (boundary=true)")
    void pointOnEdge_returnsTrue() {
        var poly = unitSquareClosed();

        // On right edge x=1
        var onRightEdge = new Coordinate(1.0, 0.5);
        assertTrue(GeoUtilities.isPointInRegion(onRightEdge, poly));

        // On bottom edge y=0
        var onBottomEdge = new Coordinate(0.5, 0.0);
        assertTrue(GeoUtilities.isPointInRegion(onBottomEdge, poly));

        // On left edge x=0
        var onLeftEdge = new Coordinate(0.0, 0.5);
        assertTrue(GeoUtilities.isPointInRegion(onLeftEdge, poly));
    }

    @Test
    @DisplayName("FR-U3: point on a vertex is treated as inside (boundary=true)")
    void pointOnVertex_returnsTrue() {
        var poly = unitSquareClosed();

        var v0 = new Coordinate(0.0, 0.0);
        var v1 = new Coordinate(1.0, 0.0);
        var v2 = new Coordinate(1.0, 1.0);
        var v3 = new Coordinate(0.0, 1.0);

        assertTrue(GeoUtilities.isPointInRegion(v0, poly));
        assertTrue(GeoUtilities.isPointInRegion(v1, poly));
        assertTrue(GeoUtilities.isPointInRegion(v2, poly));
        assertTrue(GeoUtilities.isPointInRegion(v3, poly));
    }

    @Test
    @DisplayName("FR-U3: point collinear with an edge but outside the segment returns false")
    void pointCollinearButOutsideSegment_returnsFalse() {
        var poly = unitSquareClosed();

        // Collinear with bottom edge y=0, but outside segment [0,1] in x
        var p = new Coordinate(1.5, 0.0);

        assertFalse(GeoUtilities.isPointInRegion(p, poly));
    }

    @Test
    @DisplayName("FR-U3 contract boundary: null inputs throw (documented behaviour)")
    void nullInputs_throw() {
        var poly = unitSquareClosed();
        var p = new Coordinate(0.5, 0.5);

        assertThrows(NullPointerException.class, () -> GeoUtilities.isPointInRegion(null, poly));
        assertThrows(NullPointerException.class, () -> GeoUtilities.isPointInRegion(p, null));
        assertThrows(NullPointerException.class, () -> GeoUtilities.isPointInRegion(null, null));
    }

    @Test
    @DisplayName("FR-U3: point with y equal to a vertex y-level is handled correctly (ray-vertex edge case)")
    void pointOnVertexYLevel_edgeCase_isCorrect() {
        var poly = unitSquareClosed();

        // y==0 (same as bottom vertices). Outside to the right: should be false.
        // This hits the common branch where algorithms avoid double-counting a vertex intersection.
        var outsideOnVertexY = new Coordinate(1.5, 0.0);
        assertFalse(GeoUtilities.isPointInRegion(outsideOnVertexY, poly));

        // y==1 (same as top vertices). Outside to the left: should be false.
        var outsideOnTopVertexY = new Coordinate(-0.5, 1.0);
        assertFalse(GeoUtilities.isPointInRegion(outsideOnTopVertexY, poly));
    }

    @Test
    @DisplayName("FR-U3: horizontal-edge handling is correct (on-edge true, collinear-outside false)")
    void horizontalEdgeHandling_isCorrect() {
        var poly = unitSquareClosed();

        // On the top horizontal edge: must be true (boundary treated as inside)
        var onTopEdge = new Coordinate(0.25, 1.0);
        assertTrue(GeoUtilities.isPointInRegion(onTopEdge, poly));

        // Collinear with the top edge y=1 but outside the segment in x: must be false
        var collinearOutsideTop = new Coordinate(1.25, 1.0);
        assertFalse(GeoUtilities.isPointInRegion(collinearOutsideTop, poly));
    }

}
