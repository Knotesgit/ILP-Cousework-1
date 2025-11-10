package uk.ac.ed.acp.cw2.service;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.utility.PathFindingHelper;

import java.util.*;


@Service
public class GeoServiceImpl implements GeoService {
    private final double STEP = 0.00015;

    // Numerical epsilon for floating-point comparisons only.
    private final double EPSILON = 1e-12;

    private final PathFindingHelper pathFindingHelper = new PathFindingHelper();
    // 16 directions
    private final double[] ANGLES =
            java.util.stream.IntStream.range(0, 16)
                    .mapToDouble(i -> i * 22.5)
                    .toArray();

    // Check whether two pos are near (distance < 0.00015)
    @Override
    public boolean isNear(Coordinate pos1, Coordinate pos2) {
        return ((distanceBetween(pos1, pos2) + EPSILON) < 0.00015);
    }

    // Check whether the point a is on segment pq (inclusive)
    @Override
    public boolean onSegment(Coordinate a, Coordinate p, Coordinate q) {
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
    @Override
    public boolean isPointInRegion(Coordinate p, List<Coordinate> vertices) {
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

    // Computes Euclidean distance between two coordinates
    @Override
    public double distanceBetween(Coordinate pos1, Coordinate pos2) {
        double dx = pos1.getLng()-pos2.getLng();
        double dy = pos1.getLat()-pos2.getLat();
        return Math.sqrt(dx*dx+dy*dy);
    }

    // Compute the next coordinate from a start position and an angle.
   @Override
   public Coordinate nextPosition(Coordinate start, double angle){
       double rad = Math.toRadians(angle);
       double dx = STEP * Math.cos(rad);
       double dy = STEP * Math.sin(rad);

       Coordinate next = new Coordinate();
       next.setLng(start.getLng() + dx);
       next.setLat(start.getLat() + dy);
       return  next;
   }



   // Determine if a single movement step intersects any restricted polygon.
   @Override
   public boolean stepBlocked(Coordinate from, Coordinate to,
                              List<List<Coordinate>> rects, List<BoundBox> rectBoxes){
        if(rects == null || rects.isEmpty())
            return false;
        if(rectBoxes == null || rectBoxes.isEmpty())
            return false;
       for (int k = 0; k < rects.size(); k++){
           var poly = rects.get(k);
           var box  = rectBoxes.get(k);

           if (!pathFindingHelper.triggerByBoxOR(from, to, box)) continue;

           if (isPointInRegion(from, poly) || isPointInRegion(to, poly)) return true;

           for (int i = 0; i < poly.size() - 1; i++){
               if (segmentsIntersect(from, to, poly.get(i), poly.get(i+1))) return true;
           }
       }
       return false;
   }


   // A* search on a fixed STEP grid with 16-direction branching and Euclidean heuristic.
   @Override
   public List<Coordinate> pathBetween(
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
        Node s = new Node(start, 0, heuristic(start, goal), null);
        open.add(s);
        bestG.put(PathFindingHelper.keyOf(start), 0);
        // Main A* loop
        while (!open.isEmpty()) {
            Node cur = open.poll();

            if (isNear(cur.getP(), goal)) {
                return pathFindingHelper.reconstruct(cur);
            }
            for (double ang : ANGLES) {
                Coordinate rawNext = nextPosition(cur.getP(), ang);
                Coordinate nxt = PathFindingHelper.normalize(rawNext);
                // Obstacle check for this step
                if (stepBlocked(cur.getP(), nxt, rects, rectBoxes)) continue;
                int ng = cur.getG() + 1;
                String k = PathFindingHelper.keyOf(nxt);
                Integer old = bestG.get(k);
                if (old != null && old <= ng) continue;
                int h = heuristic(nxt, goal);
                Node nn = new Node(nxt, ng, ng + h, cur);
                bestG.put(k, ng);
                open.add(nn);
            }
        }
        return List.of();
    }

    // Segment-segment intersection with collinearity handling.
    private boolean segmentsIntersect(Coordinate p1, Coordinate p2, Coordinate q1, Coordinate q2) {
        int o1 = pathFindingHelper.orient(p1, p2, q1), o2 = pathFindingHelper.orient(p1, p2, q2);
        int o3 = pathFindingHelper.orient(q1, q2, p1), o4 = pathFindingHelper.orient(q1, q2, p2);
        if (o1 != o2 && o3 != o4) return true;
        if (o1 == 0 && onSegment(q1, p1, p2)) return true;
        if (o2 == 0 && onSegment(q2, p1, p2)) return true;
        if (o3 == 0 && onSegment(p1, q1, q2)) return true;
        if (o4 == 0 && onSegment(p2, q1, q2)) return true;
        return false;
    }


    // Admissible heuristic: optimistic step count = floor(distance / STEP)
    private int heuristic(Coordinate a, Coordinate b){
        double d = distanceBetween(a,b);
        return (int) Math.floor(d / STEP);
    }
}
