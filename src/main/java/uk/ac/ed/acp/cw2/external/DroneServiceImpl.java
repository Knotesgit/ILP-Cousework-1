package uk.ac.ed.acp.cw2.external;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import uk.ac.ed.acp.cw2.data.*;

import java.util.List;
@Service
public class DroneServiceImpl implements DroneService {
    private final IlpClientComponent ilpClient;

    public DroneServiceImpl(IlpClientComponent ilpClient) {
        this.ilpClient = ilpClient;
    }

    @Override
    public List<Integer> getDronesWithCooling(@PathVariable boolean state){
        List<Drone> drones = ilpClient.getAllDrones();
        List<Integer> ids = drones.stream()
                .filter(d -> d.getCapability().isCooling() == state)
                .map(Drone::getId)
                .toList();
        return ids;
    }
}
