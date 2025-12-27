package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.BoundBox;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.RestrictedArea;
import uk.ac.ed.acp.cw2.utility.DeliveryPlanHelper;
import uk.ac.ed.acp.cw2.utility.PathFindingHelper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathFindingHelperStepBlockedTest {

    private static final double STEP = 0.00015;

    private static Coordinate c(double lng, double lat) {
        return new Coordinate(lng, lat);
    }

    private static RestrictedArea ra(int id, List<Coordinate> closedPoly) {
        RestrictedArea r = new RestrictedArea();
        r.setId(id);
        r.setName("RA-" + id);
        r.setVertices(closedPoly);
        return r;
    }

    private static List<Coordinate> closedRect(double minX, double minY, double maxX, double maxY) {
        return List.of(
                c(minX, minY),
                c(maxX, minY),
                c(maxX, maxY),
                c(minX, maxY),
                c(minX, minY)
        );
    }

    @Test
    @DisplayName("stepBlocked: returns true when destination point lies inside a restricted polygon")
    void stepBlocked_true_whenDestinationInside() {
        // Rectangle thickness >= 2*STEP to avoid discretization blind spots.
        List<Coordinate> rect = closedRect(2 * STEP, -STEP, 4 * STEP, STEP);
        RestrictedArea area = ra(1, rect);

        List<List<Coordinate>> rects = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> rectBoxes = DeliveryPlanHelper.extractBBoxes(List.of(area));

        Coordinate from = c(1 * STEP, 0.0);
        Coordinate to   = c(3 * STEP, 0.0); // clearly inside

        assertTrue(PathFindingHelper.stepBlocked(from, to, rects, rectBoxes));
    }

    @Test
    @DisplayName("stepBlocked: returns false when both endpoints are clearly outside and not near any vertex")
    void stepBlocked_false_whenClearlyOutside() {
        List<Coordinate> rect = closedRect(2 * STEP, -STEP, 4 * STEP, STEP);
        RestrictedArea area = ra(1, rect);

        List<List<Coordinate>> rects = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> rectBoxes = DeliveryPlanHelper.extractBBoxes(List.of(area));

        Coordinate from = c(0.0, 5 * STEP);
        Coordinate to   = c(1 * STEP, 5 * STEP);

        assertFalse(PathFindingHelper.stepBlocked(from, to, rects, rectBoxes));
    }


    @Test
    @DisplayName("stepBlocked: returns true for a boundary-crossing step near a polygon vertex (forces segmentsIntersect branch)")
    void stepBlocked_true_whenCrossingNearVertex() {
        RestrictedArea area = ra(1, closedRect(2 * STEP, -STEP, 4 * STEP, STEP));
        List<List<Coordinate>> rects = DeliveryPlanHelper.extractPolygons(List.of(area));
        List<BoundBox> rectBoxes = DeliveryPlanHelper.extractBBoxes(List.of(area));

        // Target the bottom-left corner (2*STEP, -STEP).
        // Construct a segment that crosses the rectangle boundary close to this vertex
        // so that nearAnyVertex(from/to, rect) should trigger the intersection check.
        double vx =  2*STEP;
        double vy = -STEP;

        Coordinate from = c(vx - 0.5 * STEP, vy - 0.5 * STEP);
        Coordinate to   = c(vx + 0.5 * STEP, vy + 0.5 * STEP);

        assertTrue(PathFindingHelper.stepBlocked(from, to, rects, rectBoxes));
    }

    @Test
    @DisplayName("stepBlocked: returns true if any polygon blocks the step (OR semantics across restricted areas)")
    void stepBlocked_true_whenAnyPolygonBlocks() {
        RestrictedArea a1 = ra(1, closedRect(2 * STEP, -STEP, 4 * STEP, STEP));
        RestrictedArea a2 = ra(2, closedRect(10 * STEP, -STEP, 12 * STEP, STEP));

        var rects = DeliveryPlanHelper.extractPolygons(List.of(a1, a2));
        var rectBoxes = DeliveryPlanHelper.extractBBoxes(List.of(a1, a2));

        Coordinate from = c(1 * STEP, 0.0);
        Coordinate to   = c(3 * STEP, 0.0); // inside a1

        assertTrue(PathFindingHelper.stepBlocked(from, to, rects, rectBoxes));
    }
}

