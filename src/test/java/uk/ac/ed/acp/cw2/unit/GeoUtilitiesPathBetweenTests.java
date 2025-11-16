package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.BoundBox;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.utility.GeoUtilities;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavioural tests for GeoUtilities.pathBetween.
 *
 * Focus:
 *  - basic properties of returned paths (start/goal, step length)
 *  - behaviour when there is no restricted area
 *  - behaviour when start/goal lie inside restricted areas
 *  - behaviour when a polygon blocks the straight line but a detour exists
 */
class GeoUtilitiesPathBetweenTests {

    // STEP in GeoUtilities is 0.00015; we reuse it here to derive rough expectations.
    private static final double STEP = 0.00015;

    // --------- Helpers to build polygons and bounding boxes ----------

    /** Build a closed axis-aligned rectangle polygon:
     * (xmin,ymin) →
     * (xmax,ymin) →
     * (xmax,ymax) →
     * (xmin,ymax) →
     * back to first. */
    private List<Coordinate> rectPoly(double xmin, double ymin, double xmax, double ymax) {
        List<Coordinate> poly = new ArrayList<>();
        poly.add(new Coordinate(xmin, ymin));
        poly.add(new Coordinate(xmax, ymin));
        poly.add(new Coordinate(xmax, ymax));
        poly.add(new Coordinate(xmin, ymax));
        poly.add(new Coordinate(xmin, ymin)); // closed
        return poly;
    }

    /** Build a bounding box consistent with DeliveryPlanHelper.polyBox: max first, then min. */
    private BoundBox bboxFor(List<Coordinate> poly) {
        double minLng = Double.POSITIVE_INFINITY, minLat = Double.POSITIVE_INFINITY;
        double maxLng = Double.NEGATIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        for (Coordinate c : poly) {
            minLng = Math.min(minLng, c.getLng());
            maxLng = Math.max(maxLng, c.getLng());
            minLat = Math.min(minLat, c.getLat());
            maxLat = Math.max(maxLat, c.getLat());
        }
        return new BoundBox(
                new Coordinate(maxLng, maxLat), // max
                new Coordinate(minLng, minLat)  // min
        );
    }

    /** Convenience: single-rectangle restricted area lists. */
    private List<List<Coordinate>> rects(List<Coordinate> poly) {
        List<List<Coordinate>> list = new ArrayList<>();
        list.add(poly);
        return list;
    }

    private List<BoundBox> boxes(BoundBox box) {
        List<BoundBox> list = new ArrayList<>();
        list.add(box);
        return list;
    }

    /** Assert all successive steps are roughly STEP apart (within tolerance). */
    private void assertStepLengthsReasonable(List<Coordinate> path) {
        for (int i = 1; i < path.size(); i++) {
            double d = GeoUtilities.distanceBetween(path.get(i - 1), path.get(i));
            // allow some tolerance for quantization and isNear shortcut
            assertTrue(Math.abs(d - STEP) < 1e-12,
                    "Step " + i + " has unreasonable length: " + d);
        }
    }

    // ---------- P1: no obstacles ----------

    @Test
    void startEqualsGoal_returnsSinglePointPath() {
        Coordinate s = new Coordinate(0.0, 0.0);
        Coordinate g = new Coordinate(0.0, 0.0);

        List<Coordinate> path = GeoUtilities.pathBetween(s, g, null, null);

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Path should not be empty when start==goal");
        assertEquals(1, path.size(), "Path for identical start/goal should have one point");
        assertTrue(GeoUtilities.isNear(path.get(0), s));
    }

    @Test
    void straightLine_noObstacles_reasonableLengthAndEndpoints() {
        // Horizontal move of roughly 10 steps
        Coordinate s = new Coordinate(0.0, 0.0);
        double dx = 10 * STEP;
        Coordinate g = new Coordinate(dx, 0.0);

        List<Coordinate> path = GeoUtilities.pathBetween(s, g, null, null);

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Path should not be empty without obstacles");
        assertTrue(GeoUtilities.isNear(path.get(0), s),
                "First point should be near start");
        assertTrue(GeoUtilities.isNear(path.getLast(), g),
                "Last point should be near goal");

        int steps = path.size() - 1;
        double dist = GeoUtilities.distanceBetween(s, g);
        int minSteps = (int) Math.floor(dist / STEP);
        // Allow some slack
        assertTrue(steps >= minSteps,
                "Steps should be at least floor(distance/STEP)");
        assertTrue(steps <= minSteps + 5,
                "Steps should not be excessively larger than straight-line estimate");
        assertStepLengthsReasonable(path);
    }

    // ---------- P2: start/goal inside restricted area ----------

    @Test
    void startInsideRestrictedArea_returnsEmpty() {
        // Rectangle around origin; start is inside
        List<Coordinate> poly = rectPoly(-1.0, -1.0, 1.0, 1.0);
        BoundBox box = bboxFor(poly);

        Coordinate s = new Coordinate(0.0, 0.0);    // inside
        Coordinate g = new Coordinate(5.0, 0.0);    // outside

        List<Coordinate> path = GeoUtilities.pathBetween(
                s, g,
                rects(poly), boxes(box)
        );

        assertNotNull(path);
        assertTrue(path.isEmpty(), "Path should be empty when start is in restricted area");
    }

    @Test
    void goalInsideRestrictedArea_returnsEmpty() {
        // Same rectangle; goal inside
        List<Coordinate> poly = rectPoly(-1.0, -1.0, 1.0, 1.0);
        BoundBox box = bboxFor(poly);

        Coordinate s = new Coordinate(-5.0, 0.0);   // outside
        Coordinate g = new Coordinate(0.0, 0.0);    // inside

        List<Coordinate> path = GeoUtilities.pathBetween(
                s, g,
                rects(poly), boxes(box)
        );

        assertNotNull(path);
        assertTrue(path.isEmpty(), "Path should be empty when goal is in restricted area");
    }

    // ---------- P3: obstacle between start and goal but detour exists ----------

    @Test
    void obstacleBetweenStartAndGoal_pathGoesAroundAndStaysOutsidePolygon() {
        // Start left, goal right, rectangle in the middle with clear space above.
        //
        //  S -----     [####]     ----- G
        //
        Coordinate s = new Coordinate(-0.01, 0.0);
        Coordinate g = new Coordinate(0.01, 0.0);

        // Rectangle roughly centered at origin, smaller than vertical space around
        double xmin = -0.002;
        double xmax =  0.002;
        double ymin = -0.002;
        double ymax =  0.002;

        List<Coordinate> poly = rectPoly(xmin, ymin, xmax, ymax);
        BoundBox box = bboxFor(poly);

        List<Coordinate> path = GeoUtilities.pathBetween(
                s, g,
                rects(poly), boxes(box)
        );

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Path should exist around the obstacle");
        assertTrue(GeoUtilities.isNear(path.get(0), s),
                "First point should be near start");
        assertTrue(GeoUtilities.isNear(path.getLast(), g),
                "Last point should be near goal");

        // Steps should not be absurdly long compared to straight line
        double dist = GeoUtilities.distanceBetween(s, g);
        int minSteps = (int) Math.floor(dist / STEP);
        int steps = path.size() - 1;
        assertTrue(steps <= minSteps * 10,
                "Detour should not be excessively long");

        // Crucially: no path point should lie inside the obstacle polygon.
        for (Coordinate p : path) {
            boolean inside = GeoUtilities.isPointInRegion(p, poly);
            assertFalse(inside,
                    "Path point lies inside restricted polygon: " + p.getLng() + "," + p.getLat());
        }
    }

    @Test
    void diagonalMove_noObstacles_stepsAreApproximatelyStep() {
        // Roughly 10 diagonal steps (distance = 10 * STEP * sqrt(2))
        Coordinate s = new Coordinate(0.0, 0.0);
        double d = 10 * STEP;
        Coordinate g = new Coordinate(d, d);

        List<Coordinate> path = GeoUtilities.pathBetween(s, g, null, null);

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Diagonal path should exist without obstacles");
        assertTrue(GeoUtilities.isNear(path.get(0), s),
                "First point should be near start");
        assertTrue(GeoUtilities.isNear(path.getLast(), g),
                "Last point should be near goal");

        assertStepLengthsReasonable(path);
    }

    @Test
    void goalVeryClose_finalStepCanBeShorterThanStep() {
        Coordinate s = new Coordinate(0.0, 0.0);
        // goal is well within isNear radius but much closer than STEP
        Coordinate g = new Coordinate(STEP * 0.4, 0.0);

        List<Coordinate> path = GeoUtilities.pathBetween(s, g, null, null);

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Path should not be empty when goal is near");
        assertTrue(GeoUtilities.isNear(path.getLast(), g),
                "Last point should be near the goal");

        // Only check step lengths do not exceed STEP by too much
        for (int i = 1; i < path.size(); i++) {
            double d = GeoUtilities.distanceBetween(path.get(i - 1), path.get(i));
            assertTrue(d <= STEP + 1e-12,
                    "Step " + i + " must not exceed STEP by more than EPS: " + d);
        }
    }

    @Test
    void pathSkimsAroundRectangleCorner_withoutEnteringPolygon() {
        // Small rectangle near origin
        double xmin = -0.001;
        double xmax =  0.001;
        double ymin = -0.001;
        double ymax =  0.001;
        List<Coordinate> poly = rectPoly(xmin, ymin, xmax, ymax);
        BoundBox box = bboxFor(poly);

        // Start and goal chosen so that the optimal path goes just outside the corner
        Coordinate s = new Coordinate(-0.005, -0.005);
        Coordinate g = new Coordinate( 0.005,  0.005);

        List<Coordinate> path = GeoUtilities.pathBetween(
                s, g,
                rects(poly), boxes(box)
        );

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Path should exist around the corner");

        for (Coordinate p : path) {
            assertFalse(GeoUtilities.isPointInRegion(p, poly),
                    "Path must never enter the rectangle: " + p.getLng() + "," + p.getLat());
        }
    }

    @Test
    void multipleRectangles_pathAvoidsAllPolygons() {
        // Two disjoint rectangles between start and goal
        List<Coordinate> poly1 = rectPoly(-0.006, -0.002, -0.002, 0.002);
        List<Coordinate> poly2 = rectPoly( 0.002, -0.002,  0.006, 0.002);

        List<List<Coordinate>> allRects = new ArrayList<>();
        allRects.add(poly1);
        allRects.add(poly2);

        List<BoundBox> allBoxes = new ArrayList<>();
        allBoxes.add(bboxFor(poly1));
        allBoxes.add(bboxFor(poly2));

        Coordinate s = new Coordinate(-0.01, 0.0);
        Coordinate g = new Coordinate( 0.01, 0.0);

        List<Coordinate> path = GeoUtilities.pathBetween(s, g, allRects, allBoxes);

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Path should exist between multiple obstacles");

        for (Coordinate p : path) {
            assertFalse(GeoUtilities.isPointInRegion(p, poly1),
                    "Path point lies inside polygon 1");
            assertFalse(GeoUtilities.isPointInRegion(p, poly2),
                    "Path point lies inside polygon 2");
        }
    }



}
