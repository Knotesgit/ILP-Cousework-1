package uk.ac.ed.acp.cw2.external;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.acp.cw2.data.Drone;

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
        return Arrays.asList(drones);
    }
}
