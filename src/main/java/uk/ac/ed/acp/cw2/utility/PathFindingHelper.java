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

//    // Quantization scale used to normalize floating values into integer keys
//    private static final long Q = 10_000_000L;

//    // Quantize longitude to a long integer by multiplying with link Q
//
//    public static long qlng(double x) {
//        return Math.round(x * Q);
//    }
//
//    // Quantize latitude to a long integer by multiplying with link Q
//    public static long qlat(double y) {
//        return Math.round(y * Q);
//    }

    // Admissible heuristic: optimistic step count = floor(distance / STEP)
    public static int heuristic(Coordinate a, Coordinate b){
        double d = GeoUtilities.distanceBetween(a,b);
        return (int) Math.floor(d / STEP);
    }

    // Quantize longitude to a long integer by divide it with STEP
    public static long gx(Coordinate c) {
        return Math.round(c.getLng() / STEP);
    }

    // Quantize latitude to a long integer by divide it with STEP
    public static long gy(Coordinate c) {
        return Math.round(c.getLat() / STEP);
    }

//    // Generate a unique key string for a coordinate based on quantized values.
//    public static String keyOf(Coordinate c) {
//        long x = qlng(c.getLng());
//        long y = qlat(c.getLat());
//        return x + "," + y;
//    }
//
//    // Normalize a coordinate to the quantized grid.
//    public static Coordinate normalize(Coordinate c) {
//        double nx = ((double) qlng(c.getLng())) / Q;
//        double ny = ((double) qlat(c.getLat())) / Q;
//        return new Coordinate(nx, ny);
//    }


    // Key based on step grid
    public static String keyOf(Coordinate c) {
        long ix = gx(c);
        long iy = gy(c);
        return ix + "," + iy;
    }

//    public static Coordinate normalize(Coordinate c) {
//        long ix = Math.round(c.getLng() / STEP);
//        long iy = Math.round(c.getLat() / STEP);
//        return new Coordinate(ix * STEP, iy * STEP);
//    }


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

    // Reconstruct the full coordinate path by following parent pointers.
    public static List<Coordinate> reconstruct(Node t) {
        ArrayDeque<Coordinate> s = new ArrayDeque<>();
        for (Node p = t; p != null; p = p.getParent())
            s.push(p.getP());
        return new ArrayList<>(s);
    }

    public static BoundBox computeGlobalBoundBox(Coordinate start, Coordinate goal,
            List<BoundBox> rectBoxes) {
        double minX = Math.min(start.getLng(), goal.getLng());
        double maxX = Math.max(start.getLng(), goal.getLng());
        double minY = Math.min(start.getLat(),  goal.getLat());
        double maxY = Math.max(start.getLat(),  goal.getLat());

        // Merge all restricted area bound boxes
        if (rectBoxes != null) {
            for (BoundBox b : rectBoxes) {
                if (b == null || b.getMin() == null || b.getMax() == null) continue;

                minX = Math.min(minX, b.getMin().getLng());
                minY = Math.min(minY, b.getMin().getLat());
                maxX = Math.max(maxX, b.getMax().getLng());
                maxY = Math.max(maxY, b.getMax().getLat());
            }
        }

        // Add a modest margin to allow small detours outside strict hull.
        double margin = 40 * STEP; // 40 steps ~ 0.006 degrees

        return new BoundBox(
                new Coordinate(maxX + margin, maxY + margin),
                new Coordinate(minX - margin, minY - margin)
        );
    }

    // Returns true if coordinate c lies inside the given bounding box (inclusive).
    public static boolean insideBox(Coordinate c, BoundBox box) {
        double x = c.getLng();
        double y = c.getLat();
        return x >= box.getMin().getLng() &&
                x <= box.getMax().getLng() &&
                y >= box.getMin().getLat() &&
                y <= box.getMax().getLat();
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

            if (GeoUtilities.isPointInRegion(to, poly)) return true;

            if(!nearAnyVertex(from,to,poly)) continue;

            for (int i = 0; i < poly.size() - 1; i++){
                if (GeoUtilities.segmentsIntersect(from, to, poly.get(i), poly.get(i+1)))
                    return true;
            }
        }
        return false;
    }

    private static boolean nearAnyVertex(Coordinate from, Coordinate to,
            List<Coordinate> poly) {
        for (int i = 0; i < poly.size() - 1; i++) {
            var v = poly.get(i);
            if(GeoUtilities.isNear(v,from) || GeoUtilities.isNear(v,to))
                return true;
        }
        return false;
    }

}