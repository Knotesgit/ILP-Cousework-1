package uk.ac.ed.acp.cw2.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.DistanceRequest;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

import java.net.URL;
import java.time.Instant;

/**
 * Controller class that handles various HTTP endpoints for the application.
 * Provides functionality for serving the index page, retrieving a static UUID,
 * and managing key-value pairs through POST requests.
 */
@RestController()
@RequestMapping("/api/v1")
public class ServiceController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);

    @Value("${ilp.service.url}")
    public URL serviceUrl;


    @GetMapping("/")
    public String index() {
        return "<html><body>" +
                "<h1>Welcome from ILP</h1>" +
                "<h4>ILP-REST-Service-URL:</h4> <a href=\"" + serviceUrl + "\" target=\"_blank\"> " + serviceUrl+ " </a>" +
                "</body></html>";
    }

    @GetMapping("/uid")
    public String uid() {
        return "s123456";
    }

    @PostMapping("/distanceTo")
    public ResponseEntity<Double> distanceTo(@RequestBody DistanceRequest request)
    {
        if(request.getPosition1()==null||request.getPosition2()==null)
        {
            return ResponseEntity.badRequest().body(null);
        }
        Coordinate pos1 = request.getPosition1();
        Coordinate pos2 = request.getPosition2();

        //Check whether the coordinate lies in a valid range.
        if(!isValidCoordinate(pos1) || !isValidCoordinate(pos2))
        {
            return ResponseEntity.badRequest().body(null);
        }
        double dx = pos1.getLng()-pos2.getLng();
        double dy = pos1.getLat()-pos2.getLat();
        double distance = Math.sqrt(dx*dx+dy*dy);
        return ResponseEntity.ok(distance);

    }

    private boolean isValidCoordinate(Coordinate pos)
    {
        return pos != null &&
                !Double.isNaN(pos.getLat()) && !Double.isNaN(pos.getLng()) &&
                pos.getLat() >= -90 && pos.getLat() <= 90 &&
                pos.getLng() >= -180 && pos.getLng() <= 180;
    }
}
