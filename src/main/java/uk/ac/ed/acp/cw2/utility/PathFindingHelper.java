package uk.ac.ed.acp.cw2.utility;

import uk.ac.ed.acp.cw2.data.BoundBox;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Node;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

// Helper utilities used by the path finding logic.
public class PathFindingHelper {

    // Tolerance for floating-point comparisons
    private static final double EPSILON = 1e-12;

    // Quantization scale used to normalize floating values into integer keys
    private static final long Q = 1_000_000_000_000L;
    public PathFindingHelper() {
    }

    // Quantize longitude to a long integer by multiplying with link Q
    public static long qlng(double x) {
        return Math.round(x * Q);
    }

    // Quantize latitude to a long integer by multiplying with link Q
    public static long qlat(double y) {
        return Math.round(y * Q);
    }

    // Helper to check whether a value line between higher and lower bound
    public boolean axisHits(double val, double low, double high) {
        return (val >= low - EPSILON) && (val <= high + EPSILON);
    }

    // Fast bounding-box prefilter.
    // Returns true if at least one endpoint of segment (a,b) lies inside or near the bounding box.
    public boolean triggerByBoxOR(Coordinate a, Coordinate b, BoundBox box) {
        boolean xHit = axisHits(a.getLng(), box.getMin().getLng(), box.getMax().getLng())
                || axisHits(b.getLng(), box.getMin().getLng(), box.getMax().getLng());
        boolean yHit = axisHits(a.getLat(), box.getMin().getLat(), box.getMax().getLat())
                || axisHits(b.getLat(), box.getMin().getLat(), box.getMax().getLat());
        return xHit || yHit;
    }


    // Compute the orientation of the ordered triplet (a, b, c)
    public int orient(Coordinate a, Coordinate b, Coordinate c) {
        double cross = (b.getLng() - a.getLng()) * (c.getLat() - a.getLat()) - (b.getLat() - a.getLat()) * (c.getLng() - a.getLng());
        if (cross > EPSILON) return 1;
        if (cross < -EPSILON) return -1;
        return 0;
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
    public List<Coordinate> reconstruct(Node t) {
        ArrayDeque<Coordinate> s = new ArrayDeque<>();
        for (Node p = t; p != null; p = p.getParent())
            s.push(p.getP());
        return new ArrayList<>(s);
    }
}