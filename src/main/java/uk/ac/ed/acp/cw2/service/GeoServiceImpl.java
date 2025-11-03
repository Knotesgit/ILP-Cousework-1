package uk.ac.ed.acp.cw2.service;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Region;

import java.util.List;


@Service
public class GeoServiceImpl implements GeoService {
    private static final double STEP = 0.00015;

    // Numerical epsilon for floating-point comparisons only.
    private static final double EPSILON = 1e-12;

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
}
