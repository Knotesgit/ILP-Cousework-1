package uk.ac.ed.acp.cw2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.DistanceRequest;

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
    private static final double STEP = 0.00015;
    private static final double DIR_STEP = 22.5;

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
    public ResponseEntity<Double> distanceTo(@RequestBody DistanceRequest request) {
        if (request.getPosition1() == null || request.getPosition2() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        Coordinate pos1 = request.getPosition1();
        Coordinate pos2 = request.getPosition2();

        //Check whether the coordinate lies in a valid range.
        if (!isValidCoordinate(pos1) || !isValidCoordinate(pos2)) {
            return ResponseEntity.badRequest().body(null);
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
    public ResponseEntity<Boolean> isCloseTo(@RequestBody DistanceRequest request) {
        if (request.getPosition1() == null || request.getPosition2() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        Coordinate pos1 = request.getPosition1();
        Coordinate pos2 = request.getPosition2();

        if (!isValidCoordinate(pos1) || !isValidCoordinate(pos2)) {
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.ok((distanceBetween(pos1, pos2) < 0.00015));
    }

    /**
     * POST /api/v1/nextPosition
     * Calculates the next coordinate from a start position and an angle.
     * - Angle is normalized to the nearest multiple of 22.5° (16 possible directions).
     * - Each move advances by a fixed step of 0.00015° along that direction.
     * - Returns 200 with the new Coordinate as JSON on valid input.
     * - Returns 400 if the input coordinate or angle is invalid.
     */
    @PostMapping("/nextPosition")
    public ResponseEntity<Coordinate> nextPosition(@RequestBody Coordinate startPos, Double angle)
    {
        if(!isValidCoordinate(startPos) || !isValidAngle(angle))
        {
            return ResponseEntity.badRequest().body(null);
        }
        double a = normalizeTo16Dir(angle);
        double rad = Math.toRadians(a);
        double dx = STEP * Math.cos(rad);
        double dy = STEP * Math.sin(rad);
        Coordinate nextPos = new Coordinate();
        nextPos.setLng(startPos.getLng() + dx);
        nextPos.setLat(startPos.getLat() + dy);
        return ResponseEntity.ok(nextPos);
    }

    // Checks whether a coordinate is non-null and within valid lat/lng ranges
    private boolean isValidCoordinate(Coordinate pos)
    {
        return  pos.getLat() != null && pos.getLng() != null &&
                !Double.isNaN(pos.getLat()) && !Double.isNaN(pos.getLng()) &&
                Double.isFinite(pos.getLat()) && Double.isFinite(pos.getLng()) &&
                pos.getLat() >= -90 && pos.getLat() <= 90 &&
                pos.getLng() >= -180 && pos.getLng() <= 180;
    }

    // Checks whether an angle is non-null and finite.
    private boolean isValidAngle(Double angle) {
        return angle != null && !Double.isNaN(angle) && Double.isFinite(angle);
    }

    /**
     * Normalizes an arbitrary angle into one of the 16 allowed directions
     * (multiples of 22.5°).
     * Steps:
     * 1. Reduce the angle to the range [0, 360) using modulo.
     * 2. Round to the nearest multiple of DIR_STEP (22.5°).
     * 3. If the result is 360 (or slightly above due to floating-point error),
     *    wrap it back to 0.
     * This ensures the returned angle is always one of:
     * {0, 22.5, 45, 67.5, ..., 337.5}.
     */
    private double normalizeTo16Dir(double angle) {
        double a = angle % 360.0;
        if (a < 0) a += 360.0;
        double k = Math.round(a / DIR_STEP);     // 0..15
        double rounded = k * DIR_STEP;
        if (rounded >= 360.0) rounded -= 360.0;  // handle wrap-around
        return rounded;
    }


    // Computes Euclidean distance between two coordinates
    private Double distanceBetween(Coordinate pos1, Coordinate pos2)
    {
        Double dx = pos1.getLng()-pos2.getLng();
        Double dy = pos1.getLat()-pos2.getLat();
        Double distance = Math.sqrt(dx*dx+dy*dy);
        return distance;
    }
}
