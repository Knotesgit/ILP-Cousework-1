package uk.ac.ed.acp.cw2.external;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;

import java.util.Arrays;
import java.util.List;

@Component
public class IlpClientComponent {
    private final String serviceUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public IlpClientComponent(String ilpEndpoint) {
        this.serviceUrl = ilpEndpoint;
    }

    public List<Drone> getAllDrones() {
        Drone[] drones = restTemplate.getForObject(serviceUrl + "/drones", Drone[].class);
        return drones == null ? List.of() : Arrays.asList(drones);
    }
    public List<DroneForServicePoint> getDronesForServicePoints() {
        DroneForServicePoint[] arr =
                restTemplate.getForObject(serviceUrl + "/drones-for-service-points",
                        DroneForServicePoint[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    public List<ServicePoint> getServicePoints() {
        ServicePoint[] arr = restTemplate.getForObject(
                serviceUrl + "/service-points", ServicePoint[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    public List<RestrictedArea> getRestrictedAreas() {
        RestrictedArea[] arr = restTemplate.getForObject(
                serviceUrl + "/restricted-areas", RestrictedArea[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }
}
