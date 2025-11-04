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

    // Returns IDs of drones whose attributes' value matches the given query condition
    @Override
    public List<Integer> queryByAttributes(List<QueryCondition> conditions){
        List<Drone> drones = ilpClient.getAllDrones();
        return drones.stream()
                .filter(d -> matchesConditions(d, conditions))
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

    // Helper method to match a drone against a list of specific conditions
    private boolean matchesConditions(Drone d, List<QueryCondition> conditions) {
        for (QueryCondition c : conditions) {
            String attr = c.getAttribute();
            String op = c.getOperator();
            String val = c.getValue();

            switch (op) {
                case "=" -> {
                    if (!matches(d, attr, val)) return false;
                }
                case "!=" -> {
                    if (matches(d, attr, val)) return false;
                }
                case "<", ">" -> {
                    if(!compareNumeric(d, attr, op, val)) return false;
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    // Helper method that compares numeric attributes using < or > operators
    private boolean compareNumeric(Drone d, String attr, String op, String val) {
        try {
            double droneVal = switch (attr.toLowerCase()) {
                case "id" -> d.getId();
                case "capacity" -> d.getCapability().getCapacity();
                case "maxmoves" -> d.getCapability().getMaxMoves();
                case "costpermove" -> d.getCapability().getCostPerMove();
                case "costinitial" -> d.getCapability().getCostInitial();
                case "costfinal" -> d.getCapability().getCostFinal();
                default -> Double.NaN; // 非数值属性
            };
            if (Double.isNaN(droneVal)) return false;

            double queryVal = Double.parseDouble(val);
            return switch (op) {
                case "<" -> droneVal < queryVal;
                case ">" -> droneVal > queryVal;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }


}
