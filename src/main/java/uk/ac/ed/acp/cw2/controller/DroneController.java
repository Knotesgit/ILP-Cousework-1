package uk.ac.ed.acp.cw2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.external.DroneService;
import uk.ac.ed.acp.cw2.service.GeoService;
import uk.ac.ed.acp.cw2.service.ValidationService;

import java.util.List;

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
    @GetMapping("dronesWithCooling/{state}")
    public List<Integer> getDronesWithCooling(@PathVariable boolean state) {
        return droneService.getDronesWithCooling(state);
    }
}
