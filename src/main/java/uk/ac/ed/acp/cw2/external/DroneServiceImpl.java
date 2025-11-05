package uk.ac.ed.acp.cw2.external;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

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

    // Returns IDs of drones whose attributes' value matches the given query conditions
    @Override
    public List<Integer> queryByAttributes(List<QueryCondition> conditions){
        List<Drone> drones = ilpClient.getAllDrones();
        return drones.stream()
                .filter(d -> matchesConditions(d, conditions))
                .map(Drone::getId)
                .toList();
    }

    // Returns IDs of drones that are available for a list of medicine dispatch record
    @Override
    public List<Integer> queryAvailableDrones(List<MedDispatchRec> dispatches){
        if (dispatches == null || dispatches.isEmpty()) return List.of();
        for (MedDispatchRec rec : dispatches) {
            if (rec.getRequirements() == null) return List.of();
            if (rec.getRequirements().isCooling() && rec.getRequirements().isHeating()) return List.of();
        }
        int n = dispatches.size();
        List<Drone> drones = ilpClient.getAllDrones();
        List<DroneForServicePoint> dfsp = ilpClient.getDronesForServicePoints();

        Map<Integer, Map<DayOfWeek, List<Map.Entry<LocalTime, LocalTime>>>> availability =
                buildAvailabilityIndex(dfsp);

        return drones.stream()
                .filter(d -> canHandleAll(d, availability.getOrDefault(d.getId(), Map.of()), dispatches, n))
                .map(Drone::getId)
                .sorted()
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

    // Builds an index of availability windows: droneId -> DayOfWeek -> list of [from, until] time windows.
    private Map<Integer, Map<DayOfWeek, List<Map.Entry<LocalTime, LocalTime>>>>
    buildAvailabilityIndex(List<DroneForServicePoint> dfspList) {
        Map<Integer, Map<DayOfWeek, List<Map.Entry<LocalTime, LocalTime>>>> map = new HashMap<>();
        if (dfspList == null) return map;

        for (DroneForServicePoint sp : dfspList) {
            if (sp.getDrones() == null) continue;
            for (DroneForServicePoint.Item it : sp.getDrones()) {
                Map<DayOfWeek, List<Map.Entry<LocalTime, LocalTime>>> perDow =
                        map.computeIfAbsent(it.getId(), k -> new EnumMap<>(DayOfWeek.class));
                if (it.getAvailability() == null) continue;
                for (DroneForServicePoint.Availability a : it.getAvailability()) {
                    DayOfWeek dow = DayOfWeek.valueOf(a.getDayOfWeek().toUpperCase(Locale.ROOT));
                    LocalTime from = LocalTime.parse(a.getFrom());
                    LocalTime until = LocalTime.parse(a.getUntil());
                    perDow.computeIfAbsent(dow, k -> new ArrayList<>())
                            .add(new AbstractMap.SimpleEntry<>(from, until));
                }
            }
        }
        return map;
    }

    // Checks if a drone can fulfill all dispatches
    // given capacity/heating/cooling, cost bound, and availability windows.
    private boolean canHandleAll(
            Drone drone, Map<DayOfWeek, List<Map.Entry<LocalTime, LocalTime>>> windowsByDow,
            List<MedDispatchRec> dispatches, int numOfDeliveries) {
        if (drone == null || drone.getCapability() == null) return false;
        var cap = drone.getCapability();
        for (MedDispatchRec rec : dispatches) {
            var req = rec.getRequirements();
            if (cap.getCapacity() < req.getCapacity()) return false;
            if (req.isCooling() && !cap.isCooling()) return false;
            if (req.isHeating() && !cap.isHeating()) return false;
            // cost lower-bound check (fixed takeoff: initial and final cost)
            if (req.getMaxCost() != null) {
                double fixed = cap.getCostInitial() + cap.getCostFinal();
                if (fixed/numOfDeliveries > req.getMaxCost()) return false;
            }
            if (!isAvailableAt(windowsByDow, rec.getDate(), rec.getTime())) return false;
        }
        return true;
    }

    // Check if a drone has any availability window covering the given date & time
    private boolean isAvailableAt(
            Map<DayOfWeek, List<Map.Entry<LocalTime, LocalTime>>> windowsByDow,
            LocalDate date, LocalTime time
    ) {
        var ranges = windowsByDow.getOrDefault(date.getDayOfWeek(), List.of());
        for (Map.Entry<LocalTime, LocalTime> r : ranges) {
            LocalTime from = r.getKey(), until = r.getValue();
            if (!time.isBefore(from) && !time.isAfter(until)) return true; // inclusive
        }
        return false;
    }

}
