package uk.ac.ed.acp.cw2.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.BoundBox;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.RestrictedArea;
import uk.ac.ed.acp.cw2.utility.DeliveryPlanHelper;
import uk.ac.ed.acp.cw2.utility.GeoUtilities;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FR-S2 (System-level): Paths must not enter restricted areas.
 *
 * System under test:
 * - The grid-based A* pathfinding behaviour (GeoUtilities.pathBetween),
 *   which integrates movement (STEP grid), obstacle checking (stepBlocked),
 *   and search strategy.
 *
 * Oracle:
 * - Segment-level safety: every movement segment of the returned path must not
 *   intersect, touch, or enter any restricted polygon (boundary treated as blocked).
 */
class DroneServiceCalcDeliveryPathRestrictedAreaTest {

    private static final double STEP = 0.00015;

    // Keep "near" offsets safely above EPSILON=1e-12 used in GeoUtilities.
    private static final double NEAR = 1e-8;

    // ---------- geometry helpers ----------

    private static Coordinate c(double lng, double lat) {
        return new Coordinate(lng, lat);
    }

    /** Closed axis-aligned rectangle polygon (last vertex repeats the first). */
    private static List<Coordinate> closedRect(double minX, double minY, double maxX, double maxY) {
        List<Coordinate> v = new ArrayList<>();
        v.add(c(minX, minY));
        v.add(c(maxX, minY));
        v.add(c(maxX, maxY));
        v.add(c(minX, maxY));
        v.add(c(minX, minY)); // close polygon
        return v;
    }

    private static RestrictedArea ra(int id, List<Coordinate> verticesClosed) {
        RestrictedArea r = new RestrictedArea();
        r.setId(id);
        r.setName("RA-" + id);
        r.setVertices(verticesClosed);
        return r;
    }

    /**
     * Segment-level restricted-area oracle:
     * - any endpoint inside/on boundary => violation
     * - any segment intersection/touch with any polygon edge => violation
     *
     * Boundary is treated as blocked (safety semantics).
     */
    private static void assertNoRestrictedAreaViolation(List<Coordinate> path, List<List<Coordinate>> polygonsClosed) {
        assertNotNull(path);

        // Empty path is allowed only when no valid route exists (or start/goal invalid).
        // For cases where a path is required, assert a non-empty plan in the calling test.
        if (path.size() < 2) return;

        for (int i = 0; i < path.size() - 1; i++) {
            Coordinate a = path.get(i);
            Coordinate b = path.get(i + 1);

            for (List<Coordinate> poly : polygonsClosed) {
                // endpoint-in-region checks (includes boundary due to onSegment)
                assertFalse(GeoUtilities.isPointInRegion(a, poly),
                        "Violation: path point lies inside/on boundary of restricted area at index " + i);
                assertFalse(GeoUtilities.isPointInRegion(b, poly),
                        "Violation: path point lies inside/on boundary of restricted area at index " + (i + 1));

                // segment-edge intersection checks (touch/overlap count as violation)
                for (int e = 0; e < poly.size() - 1; e++) {
                    Coordinate p = poly.get(e);
                    Coordinate q = poly.get(e + 1);
                    assertFalse(GeoUtilities.segmentsIntersect(a, b, p, q),
                            "Violation: segment intersects/touches restricted boundary (segment " + i + ", edge " + e + ")");
                }
            }
        }
    }

    // ---------- S2 tests (system-level) ----------

    @Test
    @DisplayName("FR-S2-1: Baseline path exists and fully avoids restricted areas")
    void s2_1_pathAvoidsRestrictedArea_baseline() {
        // Arrange: restricted rectangle far away from the path corridor
        RestrictedArea area = ra(1, closedRect(10 * STEP, 10 * STEP, 12 * STEP, 12 * STEP));
        List<List<Coordinate>> polys = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> boxes = DeliveryPlanHelper.extractBBoxes(List.of(area));

        Coordinate start = c(0, 0);
        Coordinate goal  = c(4 * STEP, 0);

        // Act
        List<Coordinate> path = GeoUtilities.pathBetween(start, goal, polys, boxes);

        // Assert
        assertNotNull(path);
        assertFalse(path.isEmpty(), "Expected a valid path in the baseline scenario");
        assertNoRestrictedAreaViolation(path, polys);
    }

    @Test
    @DisplayName("FR-S2-2a: Start inside restricted area => path is invalid (empty)")
    void s2_2_startInsideRestrictedArea_pathInvalid() {
        // Arrange: rectangle that contains the start point
        RestrictedArea area = ra(2, closedRect(0, 0, 2 * STEP, 2 * STEP));
        List<List<Coordinate>> polys = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> boxes = DeliveryPlanHelper.extractBBoxes(List.of(area));

        Coordinate start = c(STEP, STEP);         // strictly inside
        Coordinate goal  = c(6 * STEP, 0);

        // Act
        List<Coordinate> path = GeoUtilities.pathBetween(start, goal, polys, boxes);

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty(), "Expected empty path when start is inside/on boundary of a restricted area");
    }

    @Test
    @DisplayName("FR-S2-2b: Goal inside restricted area => path is invalid (empty)")
    void s2_2_goalInsideRestrictedArea_pathInvalid() {
        // Arrange: rectangle that contains the goal point
        RestrictedArea area = ra(3, closedRect(0, 0, 2 * STEP, 2 * STEP));
        List<List<Coordinate>> polys = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> boxes = DeliveryPlanHelper.extractBBoxes(List.of(area));

        Coordinate start = c(-4 * STEP, 0);
        Coordinate goal  = c(STEP, STEP);         // strictly inside

        // Act
        List<Coordinate> path = GeoUtilities.pathBetween(start, goal, polys, boxes);

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty(), "Expected empty path when goal is inside/on boundary of a restricted area");
    }

    @Test
    @DisplayName("FR-S2-3: Direct corridor is blocked by a restricted area; " +
            "returned path must detour without crossing")
    void s2_3_endpointsOutside_butBarrierForcesDetour_noCrossing() {
        // Rectangle blocks the straight-line corridor between start and goal
        // Width/height >= STEP (as per constraint).
        RestrictedArea area = ra(4, closedRect(-STEP, -STEP, STEP, STEP));
        List<List<Coordinate>> polys = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> boxes = DeliveryPlanHelper.extractBBoxes(List.of(area));

        Coordinate start = c(-6 * STEP, 0);
        Coordinate goal  = c( 6 * STEP, 0);


        List<Coordinate> path = GeoUtilities.pathBetween(start, goal, polys, boxes);

        // Expect a non-empty plan; each segment must satisfy the restricted-area oracle.
        assertNotNull(path);
        assertFalse(path.isEmpty(), "Expected a detour path to exist around the restricted area");
        assertNoRestrictedAreaViolation(path, polys);
    }

    @Test
    @DisplayName("FR-S2-4: Goal on restricted boundary is treated as inside => path is invalid (empty)")
    void s2_4_goalOnBoundary_treatedAsInside_pathInvalid() {
        // Rectangle bottom edge y=0; goal lies on boundary edge
        RestrictedArea area = ra(5, closedRect(0, 0, 2 * STEP, 2 * STEP));
        List<List<Coordinate>> polys = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> boxes = DeliveryPlanHelper.extractBBoxes(List.of(area));

        Coordinate start = c(-2 * STEP, 0);
        Coordinate goal  = c(STEP, 0); // on bottom edge

        List<Coordinate> path = GeoUtilities.pathBetween(start, goal, polys, boxes);

        assertNotNull(path);
        assertTrue(path.isEmpty(), "Expected empty path when goal lies on restricted boundary (boundary treated as blocked)");
    }

    @Test
    @DisplayName("FR-S2-5: Goal on restricted vertex is treated as inside => path is invalid (empty)")
    void s2_5_goalOnVertex_treatedAsInside_pathInvalid() {
        // Goal lies exactly on vertex (0,0)
        RestrictedArea area = ra(6, closedRect(0, 0, 2 * STEP, 2 * STEP));
        List<List<Coordinate>> polys = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> boxes = DeliveryPlanHelper.extractBBoxes(List.of(area));

        Coordinate start = c(-2 * STEP, 0);
        Coordinate goal  = c(0, 0); // vertex


        List<Coordinate> path = GeoUtilities.pathBetween(start, goal, polys, boxes);


        assertNotNull(path);
        assertTrue(path.isEmpty(), "Expected empty path when goal lies on restricted vertex (boundary treated as blocked)");
    }

    @Test
    @DisplayName("FR-S2-6: Path near boundary but strictly outside must be accepted (no false positives)")
    void s2_6_nearBoundaryOutside_pathAccepted_noViolation() {
        // Rectangle with bottom edge y=0; goal is slightly below it (outside)
        RestrictedArea area = ra(7, closedRect(0, 0, 2 * STEP, 2 * STEP));
        List<List<Coordinate>> polys = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> boxes = DeliveryPlanHelper.extractBBoxes(List.of(area)); Coordinate start = c(-2 * STEP, -NEAR);
        Coordinate goal = c(STEP, -NEAR); // very close to boundary but outside

        List<Coordinate> path = GeoUtilities.pathBetween(start, goal, polys, boxes);

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Expected a valid path near (but outside) the restricted boundary");
        assertNoRestrictedAreaViolation(path, polys);
    }
    @Test
    @DisplayName("FR-S2-7 : Path navigates between multiple restricted areas without intersecting any")
    void s2_7_multipleAreas_corridorNavigation_noViolation() {
        // Two rectangles create a corridor around x=0 that is >= STEP wide
        // Left block: x in [-2STEP, -STEP], right block: x in [STEP, 2STEP]
        RestrictedArea left  = ra(9,  closedRect(-2 * STEP, -STEP, -STEP, STEP));
        RestrictedArea right = ra(10, closedRect( STEP, -STEP,  2 * STEP, STEP));

        List<RestrictedArea> areas = List.of(left, right);
        List<List<Coordinate>> polys = DeliveryPlanHelper.extractPolygons(areas);
        List<BoundBox> boxes = DeliveryPlanHelper.extractBBoxes(areas);

        Coordinate start = c(-3*STEP, -3 * STEP);
        Coordinate goal  = c(3*STEP,  3 * STEP);

        List<Coordinate> path = GeoUtilities.pathBetween(start, goal, polys, boxes);

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Expected a valid path through the corridor between restricted areas");
        assertNoRestrictedAreaViolation(path, polys);
    }
}

