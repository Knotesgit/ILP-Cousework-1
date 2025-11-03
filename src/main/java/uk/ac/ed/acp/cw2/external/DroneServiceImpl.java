package uk.ac.ed.acp.cw2.external;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.*;

import java.util.List;
@Service
public class DroneServiceImpl implements DroneService {
    private final IlpClientComponent ilpClient;

    public DroneServiceImpl(IlpClientComponent ilpClient) {
        this.ilpClient = ilpClient;
    }

    // Returns drone IDs filtered by cooling capability.
    @Override
    public List<Integer> getDronesWithCooling(boolean state){
        List<Drone> drones = ilpClient.getAllDrones();
        List<Integer> ids = drones.stream()
                .filter(d -> d.getCapability().isCooling() == state)
                .map(Drone::getId)
                .toList();
        return ids;
    }

    // Returns the drone matching the given ID or null if not found
    @Override
    public Drone getDroneDetails(int id){
        List<Drone> drones = ilpClient.getAllDrones();
        Drone drone = drones.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
        return drone;
    };
}
