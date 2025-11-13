package uk.ac.ed.acp.cw2.utility;

import uk.ac.ed.acp.cw2.data.BoundBox;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Node;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

// Helper utilities used by the path finding logic.
public class PathFindingHelper {
    private static final double STEP = 0.00015;

    // Tolerance for floating-point comparisons
    private static final double EPSILON = 1e-12;

    // Quantization scale used to normalize floating values into integer keys
    private static final long Q = 10_000_000L;

    // Quantize longitude to a long integer by multiplying with link Q
    public static long qlng(double x) {
        return Math.round(x * Q);
    }

    // Quantize latitude to a long integer by multiplying with link Q
    public static long qlat(double y) {
        return Math.round(y * Q);
    }

    // Admissible heuristic: optimistic step count = floor(distance / STEP)
    public static int heuristic(Coordinate a, Coordinate b){
        double d = GeoUtilities.distanceBetween(a,b);
        return (int) Math.floor(d / STEP);
    }

    // Helper to check whether a value line between higher and lower bound
    public static boolean axisHits(double val, double low, double high) {
        return (val >= low - EPSILON) && (val <= high + EPSILON);
    }

    // Fast bounding-box prefilter.
    // Returns true if at least one endpoint of segment (a,b) lies inside or near the bounding box.
    public static boolean triggerByBoxOR(Coordinate a, Coordinate b, BoundBox box) {
        boolean xHit = axisHits(a.getLng(), box.getMin().getLng(), box.getMax().getLng())
                || axisHits(b.getLng(), box.getMin().getLng(), box.getMax().getLng());
        boolean yHit = axisHits(a.getLat(), box.getMin().getLat(), box.getMax().getLat())
                || axisHits(b.getLat(), box.getMin().getLat(), box.getMax().getLat());
        return xHit || yHit;
    }

    // Generate a unique key string for a coordinate based on quantized values.
    public static String keyOf(Coordinate c) {
        long x = qlng(c.getLng());
        long y = qlat(c.getLat());
        return x + "," + y;
    }

    // Normalize a coordinate to the quantized grid.
    public static Coordinate normalize(Coordinate c) {
        double nx = ((double) qlng(c.getLng())) / Q;
        double ny = ((double) qlat(c.getLat())) / Q;
        return new Coordinate(nx, ny);
    }

    // Reconstruct the full coordinate path by following parent pointers.
    public static List<Coordinate> reconstruct(Node t) {
        ArrayDeque<Coordinate> s = new ArrayDeque<>();
        for (Node p = t; p != null; p = p.getParent())
            s.push(p.getP());
        return new ArrayList<>(s);
    }

    // Determine if a single movement step intersects any restricted polygon.
    public static boolean stepBlocked(Coordinate from, Coordinate to,
                                      List<List<Coordinate>> rects, List<BoundBox> rectBoxes){
        if(rects == null || rects.isEmpty())
            return false;
        if(rectBoxes == null || rectBoxes.isEmpty())
            return false;
        for (int k = 0; k < rects.size(); k++){
            var poly = rects.get(k);
            var box  = rectBoxes.get(k);

            if (!triggerByBoxOR(from, to, box)) continue;

            if (GeoUtilities.isPointInRegion(from, poly) ||GeoUtilities.isPointInRegion(to, poly)) return true;

            for (int i = 0; i < poly.size() - 1; i++){
                if (GeoUtilities.segmentsIntersect(from, to, poly.get(i), poly.get(i+1))) return true;
            }
        }
        return false;
    }



}