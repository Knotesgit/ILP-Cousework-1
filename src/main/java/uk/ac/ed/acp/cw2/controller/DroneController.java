package uk.ac.ed.acp.cw2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.external.DroneService;

import java.util.List;

/**
 * REST controller providing endpoints for drone-related queries.
 * This class handles the CW2 static query: dronesWithCooling/{state}.
 */
@RestController()
@RequestMapping("/api/v1")
public class DroneController {

    private static final Logger logger = LoggerFactory.getLogger(GeometryController.class);

    private final DroneService droneService;
    private final String serviceUrl;

    public DroneController(DroneService droneService, String ilpEndpoint) {
        this.droneService = droneService;
        this.serviceUrl = ilpEndpoint;
    }

    /**
     * GET /api/v1/dronesWithCooling/{state}
     * Returns all drone IDs that either support or do not support cooling.
     * - Returns 200 OK with a JSON array of matching drone IDs.
     */
    @GetMapping("dronesWithCooling/{state}")
    public List<Integer> dronesWithCooling(@PathVariable boolean state) {
        return droneService.getDronesWithCooling(state);
    }


    /**
     * GET /api/v1/droneDetails/{id}
     * Returns detailed information for a specific drone.
     * - Returns 200 OK with a Drone object if the ID exists.
     * - Returns 404 Not Found if no drone matches the given ID.
     */
    @GetMapping("droneDetails/{id}")
    public ResponseEntity<Drone> droneDetails(@PathVariable int id) {
        Drone drone = droneService.getDroneDetails(id);
        if (drone == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(drone);
    }

    /**
     * GET /api/v1/queryByAttribute/{attribute}/{value}
     * Returns all drone IDs whose given attribute matches the specified value.
     * - Returns 200 OK with a JSON array of matching drone IDs.
     */
    @GetMapping("queryAsPath/{attribute}/{value}")
    public  List<Integer> queryAsPath(@PathVariable String attribute, @PathVariable String value) {
        return droneService.getDronesByAttribute(attribute,value);
    }
}
