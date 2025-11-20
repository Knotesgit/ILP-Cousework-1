package uk.ac.ed.acp.cw2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.data.response.GeoJsonResponseCollection;
import uk.ac.ed.acp.cw2.external.DroneService;

import java.util.List;

/**
 * Controller providing endpoints for drone-related queries.
 * All use /api/v1 and return 200, or 404 as specified.
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
    @GetMapping("/dronesWithCooling/{state}")
    public List<String> dronesWithCooling(@PathVariable Boolean state) {

        return droneService.dronesWithCooling(state);
    }


    /**
     * GET /api/v1/droneDetails/{id}
     * Returns detailed information for a specific drone.
     * - Returns 200 OK with a Drone object if the ID exists.
     * - Returns 404 Not Found if no drone matches the given ID.
     */
    @GetMapping("/droneDetails/{id}")
    public ResponseEntity<Drone> droneDetails(@PathVariable String id) {
        Drone drone = droneService.droneDetails(id);
        if (drone == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(drone);
    }

    /**
     * GET /api/v1/queryAsPath/{attribute}/{value}
     * Returns all drone IDs whose given attribute matches the specified value.
     * - Returns 200 OK with a JSON array of matching drone IDs.
     */
    @GetMapping("/queryAsPath/{attribute}/{value}")
    public List<String> queryAsPath(@PathVariable String attribute, @PathVariable String value) {
        return droneService.queryAsPath(attribute,value);
    }

    /**
     * POST /api/v1/query
     * Returns all drone IDs whose attributes' value matches the given query conditions
     * - Returns 200 OK with a JSON array of matching drone IDs.
     */
    @PostMapping("/query")
    public List<String> query(@RequestBody List<QueryCondition> conditions) {
        return droneService.query(conditions);
    }

    /**
     * POST /api/v1/queryAvailableDrones
     * Returns all drone IDs that can fulfill the given list of medical dispatch records.
     * - Returns 200 OK with a JSON array of available drone IDs.
     */
    @PostMapping("/queryAvailableDrones")
    public List<String> queryAvailableDrones(@RequestBody List<MedDispatchRec> dispatches) {
        return droneService.queryAvailableDrones(dispatches);
    }

    /**
     * POST /api/v1/calcDeliveryPath
     * - 200 OK with a CalcDeliveryPathResponse JSON body describing all planned drone paths.
     * - Returns an empty plan (zero cost/moves) if no valid routes are possible.
     */
    @PostMapping("/calcDeliveryPath")
    public CalcDeliveryPathResponse calcDeliveryPath(@RequestBody List<MedDispatchRec> dispatches){
        return droneService.calcDeliveryPath(dispatches);
    }

    /**
     * POST /api/v1/calcDeliveryPathAsGeoJson
     * - 200 OK with a GeoJsonResponse JSON body describing a single drone paths.
     * - Returns an empty plan (zero cost/moves) if no valid routes are possible.
     */
    @PostMapping("/calcDeliveryPathAsGeoJson")
    public GeoJsonResponseCollection calcDeliveryPathAsGeoJson(@RequestBody List<MedDispatchRec> dispatches){
        return droneService.calcDeliveryPathAsGeoJson(dispatches);
    }
}
