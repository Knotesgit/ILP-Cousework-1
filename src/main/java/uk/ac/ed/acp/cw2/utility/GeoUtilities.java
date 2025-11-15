package uk.ac.ed.acp.cw2.utility;

import uk.ac.ed.acp.cw2.data.BoundBox;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Node;

import java.util.*;

public class GeoUtilities {

    private static final double STEP = 0.00015;

    // Numerical epsilon for floating-point comparisons only.
    private static final double EPSILON = 1e-12;

    // Prevent memory explosion and infinite expansion in A* search
    private static final int EXPANSION_CAP = 1_000_000;

    // 16 directions
    private static final double[] ANGLES =
            java.util.stream.IntStream.range(0, 16)
                    .mapToDouble(i -> i * 22.5)
                    .toArray();
    private static final double[] DX = new double[16];
    private static final double[] DY = new double[16];

    static {
        for (int i = 0; i < 16; i++) {
            double rad = Math.toRadians(i * 22.5); // 0, 22.5, 45, ...
            DX[i] = STEP * Math.cos(rad);
            DY[i] = STEP * Math.sin(rad);
        }
    }
    public static boolean isNear(Coordinate pos1, Coordinate pos2) {
        return ((distanceBetween(pos1, pos2) + EPSILON) < 0.00015);
    }

    public static double distanceBetween(Coordinate pos1, Coordinate pos2) {
        double dx = pos1.getLng() - pos2.getLng();
        double dy = pos1.getLat() - pos2.getLat();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static Coordinate nextPosition(Coordinate start, double angle){
        double rad = Math.toRadians(angle);
        double dx = STEP * Math.cos(rad);
        double dy = STEP * Math.sin(rad);

        Coordinate next = new Coordinate();
        next.setLng(start.getLng() + dx);
        next.setLat(start.getLat() + dy);
        return  next;
    }
    public static boolean onSegment(Coordinate a, Coordinate p, Coordinate q) {
        // cross product to test collinearity
        // 2D cross product (z-component of pq × pa)
        double cross = (q.getLng()-p.getLng())*(a.getLat()-p.getLat()) - (q.getLat()-p.getLat())*(a.getLng()-p.getLng());
        if (Math.abs(cross) > EPSILON) return false;

        // bounding box check with tolerance
        return Math.min(p.getLng(), q.getLng()) - EPSILON <= a.getLng() &&
                a.getLng() <= Math.max(p.getLng(), q.getLng()) + EPSILON &&
                Math.min(p.getLat(), q.getLat()) - EPSILON <= a.getLat() &&
                a.getLat() <= Math.max(p.getLat(), q.getLat()) + EPSILON;
    }

    // Check whether a point is in a region
    public static boolean isPointInRegion(Coordinate p, List<Coordinate> vertices) {
        int n = vertices.size() - 1; // polygon closed, last == first

        // check boundary and vertices
        for (int i = 0; i < n; i++) {
            if (onSegment(p, vertices.get(i), vertices.get(i+1))) return true;
        }

        boolean inside = false;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            Coordinate vi = vertices.get(i), vj = vertices.get(j);

            // ray crosses edge in y-range
            boolean crossesY = (vi.getLat() > p.getLat()) != (vj.getLat() > p.getLat());
            if (!crossesY) continue;

            // x-coordinate of intersection
            double xIntersect = (vj.getLng() - vi.getLng()) * (p.getLat() - vi.getLat())
                    / (vj.getLat() - vi.getLat()) + vi.getLng();

            // count only if intersection is to the right
            if (p.getLng() < xIntersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    // A* search on a fixed STEP grid with 16-direction branching and Euclidean heuristic.
    public static List<Coordinate> pathBetween(
            Coordinate start, Coordinate goal,
            List<List<Coordinate>> rects, List<BoundBox> rectBoxes) {
        start = PathFindingHelper.normalize(start);
        goal  = PathFindingHelper.normalize(goal);
        // Early exit if start or goal lies within any restricted area
        if (rects != null && !rects.isEmpty()){
            for(List<Coordinate> poly: rects){
                if(isPointInRegion(start, poly) || isPointInRegion(goal, poly))
                    return List.of();
            }
        }
        Map<String, Integer> bestG = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(Node::getF));
        // Seed start node
        Node s = new Node(start, 0, PathFindingHelper.heuristic(start, goal), null);
        open.add(s);
        bestG.put(PathFindingHelper.keyOf(start), 0);

        int expansions = 0;
        // Main A* loop
        while (!open.isEmpty()) {
            // Exit if explored too many node;
            expansions++;
            if (expansions > EXPANSION_CAP) {
                return List.of();
            }
            Node cur = open.poll();

            if (isNear(cur.getP(), goal)) {
                return PathFindingHelper.reconstruct(cur);
            }
            for (int dir = 0; dir < DX.length; dir++) {
                Coordinate p0 = cur.getP();
                Coordinate rawNext = new Coordinate(
                        p0.getLng() + DX[dir],
                        p0.getLat() + DY[dir]
                );
                Coordinate nxt = PathFindingHelper.normalize(rawNext);
                // Obstacle check for this step
                if (PathFindingHelper.stepBlocked(cur.getP(), nxt, rects, rectBoxes)) continue;
                int ng = cur.getG() + 1;
                String k = PathFindingHelper.keyOf(nxt);
                Integer old = bestG.get(k);
                if (old != null && old <= ng) continue;
                int h = PathFindingHelper.heuristic(nxt, goal);
                Node nn = new Node(nxt, ng, ng + h, cur);
                bestG.put(k, ng);
                open.add(nn);
            }
        }
        return List.of();
    }

    // Compute the orientation of the ordered triplet (a, b, c)
    public static int orient(Coordinate a, Coordinate b, Coordinate c) {
        double cross = (b.getLng() - a.getLng()) * (c.getLat() - a.getLat()) - (b.getLat() - a.getLat()) * (c.getLng() - a.getLng());
        if (cross > EPSILON) return 1;
        if (cross < -EPSILON) return -1;
        return 0;
    }

    // Segment-segment intersection with collinearity handling.
    public static boolean segmentsIntersect(Coordinate p1, Coordinate p2, Coordinate q1, Coordinate q2) {
        int o1 = orient(p1, p2, q1), o2 = orient(p1, p2, q2);
        int o3 = orient(q1, q2, p1), o4 = orient(q1, q2, p2);
        if (o1 != o2 && o3 != o4) return true;
        if (o1 == 0 && onSegment(q1, p1, p2)) return true;
        if (o2 == 0 && onSegment(q2, p1, p2)) return true;
        if (o3 == 0 && onSegment(p1, q1, q2)) return true;
        if (o4 == 0 && onSegment(p2, q1, q2)) return true;
        return false;
    }
}
