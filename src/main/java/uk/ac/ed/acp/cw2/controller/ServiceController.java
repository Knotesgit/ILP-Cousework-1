package uk.ac.ed.acp.cw2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.*;

import java.net.URL;
import java.util.List;

/**
 * Controller class that handles various HTTP endpoints for the application.
 * Provides functionality for serving the index page, retrieving a static UUID,
 * and managing key-value pairs through POST requests.
 */
@RestController()
@RequestMapping("/api/v1")
public class ServiceController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
    private static final double STEP = 0.00015;

    // Numerical epsilon for floating-point comparisons only.
    private static final double EPSILON = 1e-12;


    @Value("${ilp.service.url}")
    public URL serviceUrl;


    @GetMapping("/")
    public String index() {
        return "<html><body>" +
                "<h1>Welcome from ILP</h1>" +
                "<h4>ILP-REST-Service-URL:</h4> <a href=\"" + serviceUrl + "\" target=\"_blank\"> " + serviceUrl + " </a>" +
                "</body></html>";
    }

    @GetMapping("/uid")
    public String uid() {
        return "s2536347";
    }


    /**
     * POST /api/v1/distanceTo
     * Calculates the Euclidean distance between two coordinates.
     * - Returns 200 with the numeric distance if both positions are valid.
     * - Returns 400 if either position is null or outside the valid lat/lng range.
     */
    @PostMapping("/distanceTo")
    public ResponseEntity<Double> distanceTo(@RequestBody DistanceRequest req) {
        Coordinate pos1 = (req != null) ? req.getPosition1() : null;
        Coordinate pos2 = (req != null) ? req.getPosition2() : null;

        //Check whether the coordinate lies in a valid range.
        if (!isValidCoordinate(pos1) || !isValidCoordinate(pos2)) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(distanceBetween(pos1, pos2));
    }

    /**
     * POST /api/v1/isCloseTo
     * Returns true if two positions are closer than 0.00015, false otherwise.
     * - Returns 200 with a boolean result for valid input.
     * - Returns 400 for null or invalid coordinates.
     */
    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isCloseTo(@RequestBody DistanceRequest req) {
        Coordinate pos1 = (req != null) ? req.getPosition1() : null;
        Coordinate pos2 = (req != null) ? req.getPosition2() : null;

        if (!isValidCoordinate(pos1) || !isValidCoordinate(pos2)) {
            return ResponseEntity.badRequest().build();
        }
        // prevent floating error
        boolean close = (distanceBetween(pos1, pos2) + EPSILON < 0.00015);
        return ResponseEntity.ok(close);
    }

    /**
     * POST /api/v1/nextPosition
     * Calculates the next coordinate from a start position and an angle.
     * - Angle must be an exact multiple of 22.5° (with tiny FP tolerance); otherwise 400.
     * - Each move advances by a fixed step of 0.00015° along that direction.
     * - Returns 200 with the new Coordinate as JSON on valid input.
     * - Returns 400 if the input coordinate or angle is invalid.
     */
    @PostMapping("/nextPosition")
    public ResponseEntity<Coordinate> nextPosition(@RequestBody NextPositionRequest req) {
        Coordinate start = (req != null) ? req.getStart() : null;
        Double angle = (req != null) ? req.getAngle() : null;

        if (!isValidCoordinate(start) || !isValidAngle(angle)) {
            return ResponseEntity.badRequest().build();
        }

        double rad = Math.toRadians(angle);
        double dx = STEP * Math.cos(rad);
        double dy = STEP * Math.sin(rad);

        Coordinate next = new Coordinate();
        next.setLng(start.getLng() + dx);
        next.setLat(start.getLat() + dy);
        return ResponseEntity.ok(next);
    }

    /**
     * POST /api/v1/isInRegion
     * Checks whether a given position lies inside a polygonal region.
     * - Region must have at least 4 vertices and be closed (last vertex equals first).
     * - A point on the polygon border counts as inside.
     * - Returns 200 with true/false on valid input.
     * - Returns 400 if the position or region data is invalid (e.g. missing, NaN, or open polygon).
     */
    @PostMapping("/isInRegion")
    public  ResponseEntity<Boolean> isInRegion(@RequestBody RegionRequest req) {
        Coordinate pos = (req != null) ? req.getPosition() : null;
        Region region = (req != null) ? req.getRegion() : null;
        if(!isValidCoordinate(pos) || !isValidRegion(region)) {
            return ResponseEntity.badRequest().build();
        }
        return  ResponseEntity.ok(isPointInRegion(pos,region.getVertices()));
    }

    // Checks whether a coordinate is non-null and within valid lat/lng ranges
    private boolean isValidCoordinate(Coordinate pos)
    {
        if (pos == null) return false;
        return  pos.getLat() != null && pos.getLng() != null &&
                !Double.isNaN(pos.getLat()) && !Double.isNaN(pos.getLng()) &&
                Double.isFinite(pos.getLat()) && Double.isFinite(pos.getLng());
    }

    // Checks whether an angle is non-null and finite.
    private boolean isValidAngle(Double angle) {
        if (angle == null || Double.isNaN(angle) || !Double.isFinite(angle)) return false;
        if (angle < 0 || angle >= 360) return false;
        double remainder = angle % 22.5;
        return Math.abs(remainder) < EPSILON || Math.abs(remainder - 22.5) < EPSILON;
    }

    // Checks whether a region is valid
    private boolean isValidRegion(Region region)
    {
        if (region == null) return false;
        List<Coordinate> v = region.getVertices();
        if (v == null || v.size() < 4) return false;
        if (!isClosed(v)) return false;
        for (Coordinate c : v) if (!isValidCoordinate(c)) return false;
        return true;
    }

    // Check whether the region(polygon) is closed
    private boolean isClosed(List<Coordinate> v) {
        if (v.size() < 2) return false;
        Coordinate a = v.get(0), b = v.get(v.size()-1);
        return Math.abs(a.getLng() - b.getLng()) <= EPSILON &&
                Math.abs(a.getLat() - b.getLat()) <= EPSILON;
    }

    // Check whether the point a is on segment pq (inclusive)
    private boolean onSegment(Coordinate a, Coordinate p, Coordinate q) {
        // cross product to test collinearity
        double cross = (q.getLng()-p.getLng())*(a.getLat()-p.getLat()) - (q.getLat()-p.getLat())*(a.getLng()-p.getLng());
        if (Math.abs(cross) > EPSILON) return false;

        // bounding box check with tolerance
        return Math.min(p.getLng(), q.getLng()) - EPSILON <= a.getLng() &&
                a.getLng() <= Math.max(p.getLng(), q.getLng()) + EPSILON &&
                Math.min(p.getLat(), q.getLat()) - EPSILON <= a.getLat() &&
                a.getLat() <= Math.max(p.getLat(), q.getLat()) + EPSILON;
    }

    // Check whether a point is in a region
    private boolean isPointInRegion(Coordinate p, List<Coordinate> vertices) {
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
    private double distanceBetween(Coordinate pos1, Coordinate pos2)
    {
        double dx = pos1.getLng()-pos2.getLng();
        double dy = pos1.getLat()-pos2.getLat();
        return Math.sqrt(dx*dx+dy*dy);
    }

}
