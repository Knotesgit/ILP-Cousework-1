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
        return "s2536347";
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

        return ResponseEntity.ok(distanceBetween(pos1, pos2));
    }

    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isCloseTo(@RequestBody DistanceRequest request)
    {
        if(request.getPosition1()==null||request.getPosition2()==null)
        {
            return ResponseEntity.badRequest().body(null);
        }
        Coordinate pos1 = request.getPosition1();
        Coordinate pos2 = request.getPosition2();

        if(!isValidCoordinate(pos1) || !isValidCoordinate(pos2))
        {
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.ok((distanceBetween(pos1,pos2)<0.00015));
    }

    private boolean isValidCoordinate(Coordinate pos)
    {
        return  pos.getLat() != null && pos.getLng() != null &&
                pos.getLat() >= -90 && pos.getLat() <= 90 &&
                pos.getLng() >= -180 && pos.getLng() <= 180;
    }
    private Double distanceBetween(Coordinate pos1, Coordinate pos2)
    {
        Double dx = pos1.getLng()-pos2.getLng();
        Double dy = pos1.getLat()-pos2.getLat();
        Double distance = Math.sqrt(dx*dx+dy*dy);
        return distance;
    }
}
