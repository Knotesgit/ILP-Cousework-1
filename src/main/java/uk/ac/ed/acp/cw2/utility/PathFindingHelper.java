package uk.ac.ed.acp.cw2.utility;

import uk.ac.ed.acp.cw2.data.BoundBox;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Node;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class PathFindingHelper {

    private static final double EPSILON = 1e-12;

    private static final long Q = 1_000_000_000_000L;
    public PathFindingHelper() {
    }

    public static long qlng(double x) {
        return Math.round(x * Q);
    }

    public static long qlat(double y) {
        return Math.round(y * Q);
    }

    // Helper to check whether a value line between higher and lower bound
    public boolean axisHits(double val, double low, double high) {
        return (val >= low - EPSILON) && (val <= high + EPSILON);
    }

    public boolean triggerByBoxOR(Coordinate a, Coordinate b, BoundBox box) {
        boolean xHit = axisHits(a.getLng(), box.getMin().getLng(), box.getMax().getLng())
                || axisHits(b.getLng(), box.getMin().getLng(), box.getMax().getLng());
        boolean yHit = axisHits(a.getLat(), box.getMin().getLat(), box.getMax().getLat())
                || axisHits(b.getLat(), box.getMin().getLat(), box.getMax().getLat());
        return xHit || yHit;
    }

    public int orient(Coordinate a, Coordinate b, Coordinate c) {
        double cross = (b.getLng() - a.getLng()) * (c.getLat() - a.getLat()) - (b.getLat() - a.getLat()) * (c.getLng() - a.getLng());
        if (cross > EPSILON) return 1;
        if (cross < -EPSILON) return -1;
        return 0;
    }//

    public static String keyOf(Coordinate c) {
        long x = qlng(c.getLng());
        long y = qlat(c.getLat());
        return x + "," + y;
    }//

    public static Coordinate normalize(Coordinate c) {
        double nx = ((double) qlng(c.getLng())) / Q;
        double ny = ((double) qlat(c.getLat())) / Q;
        return new Coordinate(nx, ny);
    }

    public List<Coordinate> reconstruct(Node t) {
        ArrayDeque<Coordinate> s = new ArrayDeque<>();
        for (Node p = t; p != null; p = p.getParent())
            s.push(p.getP());
        return new ArrayList<>(s);
    }
}