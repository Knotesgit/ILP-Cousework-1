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
        return drones.stream()
                .filter(d -> d.getCapability().isCooling() == state)
                .map(Drone::getId)
                .toList();
    }

    // Returns the drone matching the given ID or null if not found
    @Override
    public Drone getDroneDetails(int id){
        List<Drone> drones = ilpClient.getAllDrones();
        return drones.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
    }

    // Returns IDs of drones whose given attribute matches the specified value
    @Override
    public List<Integer> getDronesByAttribute(String attribute, String value){
        List<Drone> drones = ilpClient.getAllDrones();
        return drones.stream()
                .filter(d -> matches(d, attribute, value))
                .map(Drone::getId)
                .toList();
    }
    // Helper method to match a drone against a specific attribute and value
    private boolean matches(Drone d, String attr, String val) {
        try {
            return switch (attr.toLowerCase()) {
                case "id" -> d.getId() == Integer.parseInt(val);
                case "name" -> d.getName().equalsIgnoreCase(val);
                case "cooling" -> d.getCapability().isCooling() == Boolean.parseBoolean(val);
                case "heating" -> d.getCapability().isHeating() == Boolean.parseBoolean(val);
                case "capacity" -> d.getCapability().getCapacity() == Double.parseDouble(val);
                case "maxmoves" -> d.getCapability().getMaxMoves() == Integer.parseInt(val);
                case "costpermove" -> d.getCapability().getCostPerMove() == Double.parseDouble(val);
                case "costinitial" -> d.getCapability().getCostInitial() == Double.parseDouble(val);
                case "costfinal" -> d.getCapability().getCostFinal() == Double.parseDouble(val);
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false; // invalid number input
        }
    }
}
