package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.BoundBox;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.utility.GeoUtilities;
import uk.ac.ed.acp.cw2.utility.PathFindingHelper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PathFindingHelper.stepBlocked().
 * The goals:
 *  - Simulate ILP CW environment where each move has length STEP = 0.00015.
 *  - Restricted areas are rectangles with width and height > STEP.
 *  - Check that legal single steps just outside the rectangle are NOT blocked.
 *  - Check that steps entering the rectangle ARE blocked.
 * In the additional tests we also:
 *  - Use diagonal moves with length exactly STEP (as in 16-direction movement).
 *  - Pass coordinates through PathFindingHelper.normalize() before calling stepBlocked()
 *    to simulate the real pathBetween behaviour.
 */
public class PathFindingHelperStepBlockedTests {

    // ILP step size
    private static final double STEP = 0.00015;
    // Diagonal step component so that sqrt(dx^2 + dy^2) == STEP
    private static final double DIAG = STEP / Math.sqrt(2.0);

    /**
     * Helper: build a closed rectangular polygon.
     * (x1,y1) is bottom-left, (x2,y2) is top-right.
     * We ensure width and height > STEP.
     */
    private List<Coordinate> rect(double x1, double y1, double x2, double y2) {
        // closed polygon: last vertex repeats the first
        return List.of(
                new Coordinate(x1, y1),
                new Coordinate(x1, y2),
                new Coordinate(x2, y2),
                new Coordinate(x2, y1),
                new Coordinate(x1, y1)
        );
    }

    /**
     * Helper: build matching bounding box for the rectangle.
     */
    private BoundBox box(double x1, double y1, double x2, double y2) {
        BoundBox bb = new BoundBox(null, null);
        bb.setMin(new Coordinate(Math.min(x1, x2), Math.min(y1, y2)));
        bb.setMax(new Coordinate(Math.max(x1, x2), Math.max(y1, y2)));
        return bb;
    }


    /**
     * Assert that the Euclidean distance between from and to
     * is exactly one STEP (within a small epsilon).
     */
    private void assertIsSingleStep(Coordinate from, Coordinate to) {
        double d = GeoUtilities.distanceBetween(from, to);
        assertEquals(STEP, d, 1e-9,
                "Segment length must be exactly one STEP (0.00015) to simulate CW environment.");
    }

    // -------------------------------------------------------------------------
    // Original axis-aligned tests
    // -------------------------------------------------------------------------

    /**
     * Test 1:
     * A single horizontal step from outside into the rectangle boundary.
     * Rectangle:  [0, 3*STEP] x [0, 3*STEP]
     * From:       (-STEP, 1.5*STEP)  (clearly outside)
     * To:         (0,      1.5*STEP)  (on the left boundary)
     * By ILP spec, being on the boundary counts as "inside" the region,
     * so this step MUST be blocked.
     */
    @Test
    public void testStepEnteringRectangleFromOutside() {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 3 * STEP;
        double y2 = 3 * STEP;

        List<Coordinate> poly = rect(x1, y1, x2, y2);
        BoundBox bb = box(x1, y1, x2, y2);

        Coordinate from = new Coordinate(-STEP, 1.5 * STEP);
        Coordinate to   = new Coordinate(0.0,  1.5 * STEP);

        // sanity check: segment length must be exactly STEP
        assertIsSingleStep(from, to);

        boolean blocked = PathFindingHelper.stepBlocked(
                from, to,
                List.of(poly),
                List.of(bb)
        );

        assertTrue(blocked,
                "A single step whose endpoint lies on the rectangle boundary must be blocked.");
    }

    /**
     * Test 2:
     * A single horizontal step far away from the rectangle.
     * Rectangle:  [0, 3*STEP] x [0, 3*STEP]
     * Segment: completely on the left and far below, no interaction.
     * This MUST NOT be blocked.
     */
    @Test
    public void testStepCompletelyOutsideFarAway() {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 3 * STEP;
        double y2 = 3 * STEP;

        List<Coordinate> poly = rect(x1, y1, x2, y2);
        BoundBox bb = box(x1, y1, x2, y2);

        Coordinate from = new Coordinate(-3 * STEP, -3 * STEP);
        Coordinate to   = new Coordinate(-2 * STEP, -3 * STEP);

        assertIsSingleStep(from, to);

        boolean blocked = PathFindingHelper.stepBlocked(
                from, to,
                List.of(poly),
                List.of(bb)
        );

        assertFalse(blocked,
                "A single step far away from the rectangle must not be blocked.");
    }

    /**
     * Test 3:
     * A single vertical step "sliding" along the left side of the rectangle,
     * but staying strictly outside.
     * Rectangle: x in [0, 3*STEP], y in [0, 3*STEP]
     * Step:      x = -STEP (just one STEP to the left), y from STEP to 2*STEP.
     * The segment never enters the polygon, it just runs parallel to the edge.
     * This MUST NOT be blocked.
     * If this test fails, your nearAnyVertex / intersection logic is too aggressive.
     */
    @Test
    public void testStepSlidingOutsideAlongVerticalEdge() {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 3 * STEP;
        double y2 = 3 * STEP;

        List<Coordinate> poly = rect(x1, y1, x2, y2);
        BoundBox bb = box(x1, y1, x2, y2);

        // Slide along a vertical line x = -STEP, outside the rectangle
        Coordinate from = new Coordinate(-STEP, STEP);
        Coordinate to   = new Coordinate(-STEP, 2 * STEP);

        assertIsSingleStep(from, to);

        boolean blocked = PathFindingHelper.stepBlocked(
                from, to,
                List.of(poly),
                List.of(bb)
        );

        assertFalse(blocked,
                "A single step sliding outside along the rectangle edge must NOT be blocked.");
    }

    /**
     * Test 4:
     * A "corner-grazing" step: it moves near the bottom-left corner,
     * but stays strictly outside the rectangle.
     * Rectangle: [0, 3*STEP] x [0, 3*STEP]
     * Step:      x = -STEP, y from -STEP to 0
     * The segment ends exactly at y = 0 but x = -STEP (< 0), so it is still outside.
     * This MUST NOT be blocked.
     * If this is blocked, your "nearAnyVertex" / intersection filtering is wrong.
     */
    @Test
    public void testStepNearCornerButOutside() {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 3 * STEP;
        double y2 = 3 * STEP;

        List<Coordinate> poly = rect(x1, y1, x2, y2);
        BoundBox bb = box(x1, y1, x2, y2);

        // Move near the bottom-left corner (0,0), but stay outside:
        // from (-STEP, -STEP) to (-STEP, 0)
        Coordinate from = new Coordinate(-STEP, -STEP);
        Coordinate to   = new Coordinate(-STEP, 0.0);

        assertIsSingleStep(from, to);

        boolean blocked = PathFindingHelper.stepBlocked(
                from, to,
                List.of(poly),
                List.of(bb)
        );

        assertFalse(blocked,
                "A step near the corner but fully outside the polygon must NOT be blocked.");
    }

    /**
     * Test 5:
     * A single step from boundary into the *interior* of the rectangle.
     * Rectangle: [0, 3*STEP] x [0, 3*STEP]
     * From:      (STEP, 0.0)   (on bottom edge)
     * To:        (STEP, STEP)  (strictly inside)
     * This step MUST be blocked.
     */
    @Test
    public void testStepIntoInteriorFromBoundary() {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 3 * STEP;
        double y2 = 3 * STEP;

        List<Coordinate> poly = rect(x1, y1, x2, y2);
        BoundBox bb = box(x1, y1, x2, y2);

        // "from" on the boundary, "to" strictly inside
        Coordinate from = new Coordinate(STEP, 0.0);      // on bottom edge
        Coordinate to   = new Coordinate(STEP, STEP);     // inside

        assertIsSingleStep(from, to);

        boolean blocked = PathFindingHelper.stepBlocked(
                from, to,
                List.of(poly),
                List.of(bb)
        );

        assertTrue(blocked,
                "A step from boundary into the interior of the rectangle must be blocked.");
    }

}

