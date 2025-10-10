package uk.ac.ed.acp.cw2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.service.GeoService;

import java.net.URL;

/**
 * Controller class that handles various HTTP endpoints for the application.
 * Provides functionality for serving the index page, retrieving a static UUID,
 * and managing key-value pairs through POST requests.
 */
@RestController()
@RequestMapping("/api/v1")
public class ServiceController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
    private final GeoService geo;

    public ServiceController(GeoService geo) {
        this.geo = geo;
    }

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
        if (!geo.isValidCoordinate(pos1) || !geo.isValidCoordinate(pos2)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(geo.distanceBetween(pos1, pos2));
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
        if (!geo.isValidCoordinate(pos1) || !geo.isValidCoordinate(pos2)) {
            return ResponseEntity.badRequest().build();
        }
        // prevent floating error
        return ResponseEntity.ok(geo.isNear(pos1, pos2));
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

        if (!geo.isValidCoordinate(start) || !geo.isValidAngle(angle)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(geo.nextPosition(start,angle));
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
        if(!geo.isValidCoordinate(pos) || !geo.isValidRegion(region)) {
            return ResponseEntity.badRequest().build();
        }
        return  ResponseEntity.ok(geo.isPointInRegion(pos,region.getVertices()));
    }


}
