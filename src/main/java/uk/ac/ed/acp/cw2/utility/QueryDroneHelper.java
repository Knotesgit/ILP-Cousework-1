package uk.ac.ed.acp.cw2.utility;

import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class QueryDroneHelper {

    private static final double STEP = 0.00015;

    // Helper method to match a drone against a specific attribute and value
    public static boolean matches(Drone d, String attr, String val) {
        try {
            return switch (attr.toLowerCase()) {
                case "id" -> d.getId().equals(val);
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
    public static boolean matchesConditions(Drone d, List<QueryCondition> conditions) {
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
                    if (!compareNumeric(d, attr, op, val)) return false;
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    // Helper method that compares numeric attributes using < or > operators
    public static boolean compareNumeric(Drone d, String attr, String op, String val) {
        try {
            double droneVal = switch (attr.toLowerCase()) {
                case "capacity" -> d.getCapability().getCapacity();
                case "maxmoves" -> d.getCapability().getMaxMoves();
                case "costpermove" -> d.getCapability().getCostPerMove();
                case "costinitial" -> d.getCapability().getCostInitial();
                case "costfinal" -> d.getCapability().getCostFinal();
                default -> Double.NaN;
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

    // Builds an index of availability windows: droneId -> list of [from, until] time windows.
    public static Map<String, List<DroneForServicePoint.Availability>>
    buildAvailabilityIndex(List<DroneForServicePoint> dfspList) {
        Map<String, List<DroneForServicePoint.Availability>> map = new HashMap<>();
        if (dfspList == null) return map;

        for (DroneForServicePoint sp : dfspList) {
            if (sp.getDrones() == null) continue;
            for (DroneForServicePoint.Item it : sp.getDrones()) {
                List<DroneForServicePoint.Availability> list =
                        map.computeIfAbsent(it.getId(), k -> new ArrayList<>());
                if (it.getAvailability() != null) {
                    list.addAll(it.getAvailability());
                }
            }
        }
        return map;
    }

    // Builds an index of servicPoint: droneId -> list of servicePoint it available.
    public static Map<String, List<ServicePoint>> buildHomePointIndex(
            List<DroneForServicePoint> dfspList,
            List<ServicePoint> servicePoints) {

        Map<String, List<ServicePoint>> map = new HashMap<>();
        if (dfspList == null || servicePoints == null) return map;

        Map<Integer, ServicePoint> spById = new HashMap<>();
        for (ServicePoint sp : servicePoints) {
            spById.put(sp.getId(), sp);
        }

        for (DroneForServicePoint spEntry : dfspList) {
            ServicePoint sp = spById.get(spEntry.getServicePointId());
            if (sp == null || spEntry.getDrones() == null) continue;

            for (DroneForServicePoint.Item it : spEntry.getDrones()) {
                map.computeIfAbsent(it.getId(), k -> new ArrayList<>())
                        .add(sp);
            }
        }
        return map;
    }

    // Checks if a drone can fulfill all dispatches
    // given capacity/heating/cooling, cost bound, and availability windows.
    public static boolean canHandleAll(Drone drone,
                                List<DroneForServicePoint.Availability> windows,
                                List<ServicePoint> homePoints,
                                List<MedDispatchRec> dispatches,
                                int numOfDeliveries) {

        if (drone == null || drone.getCapability() == null) return false;
        if (homePoints == null || homePoints.isEmpty()) return false;

        var cap = drone.getCapability();
        double totalRequired = 0.0;
        boolean hasAnyMaxCost = false;

        for (MedDispatchRec rec : dispatches) {
            var req = rec.getRequirements();
            if (req == null || req.getCapacity() == null) return false;
            if (cap.getCapacity() < req.getCapacity()) return false;
            totalRequired += req.getCapacity();
            if (req.isCooling() && !cap.isCooling()) return false;
            if (req.isHeating() && !cap.isHeating()) return false;
            if (!isAvailableAt(windows, rec.getDate(), rec.getTime())) return false;

            if (req.getMaxCost() != null)
                hasAnyMaxCost = true;
        }
        if (totalRequired > cap.getCapacity()) return false;
        if (!hasAnyMaxCost) return true;

        return respectsMaxCost(cap, homePoints, dispatches, numOfDeliveries);
    }


    // Check whether a list of time windows contains certain data and time
    public static boolean isAvailableAt(List<DroneForServicePoint.Availability> windows,
                                        LocalDate date, LocalTime time) {
        // No date and time restriction
        if (date == null && time == null) return true;
        if (windows == null || windows.isEmpty())
            return false;
        if (date == null)
            return false;
        // Only date restriction
        if (time == null) {
            DayOfWeek dow = date.getDayOfWeek();
            for (DroneForServicePoint.Availability w : windows) {
                DayOfWeek wDow = parseDayOfWeek(w.getDayOfWeek());
                if (wDow != null && wDow == dow) return true;
            }
            return false;
        }
        // Both day and time exist
        DayOfWeek dow = date.getDayOfWeek();
        for (DroneForServicePoint.Availability w : windows) {
            DayOfWeek wDow = parseDayOfWeek(w.getDayOfWeek());
            if (wDow == null || wDow != dow) continue;
            LocalTime from = parseTimeSafe(w.getFrom());
            LocalTime until = parseTimeSafe(w.getUntil());
            if (from != null && until != null && !time.isBefore(from) && !time.isAfter(until)) {
                return true;
            }
        }
        return false;
    }

    private static boolean respectsMaxCost(Drone.DroneCapability cap,
                                    List<ServicePoint> homePoints,
                                    List<MedDispatchRec> dispatches,
                                    int numOfDeliveries) {

        double bestMoves = Double.POSITIVE_INFINITY;

        // Try each service point as base, keep the cheapest approximate route
        for (ServicePoint sp : homePoints) {
            if (sp == null || sp.getLocation() == null) continue;

            Coordinate curr = sp.getLocation();
            double moves = 0.0;

            for (MedDispatchRec rec : dispatches) {
                Coordinate target = rec.getDelivery();
                if (target == null) return false;

                double dist = GeoUtilities.distanceBetween(curr, target);
                moves += dist / STEP;
                // Hover for delivery (two identical coordinates)
                moves += 1.0;

                curr = target;
            }

            // Last delivery -> base
            double backDist = GeoUtilities.distanceBetween(curr, sp.getLocation());
            moves += backDist / STEP;

            if (moves < bestMoves) {
                bestMoves = moves;
            }
        }

        if (bestMoves == Double.POSITIVE_INFINITY) return false;

        double fixed = cap.getCostInitial() + cap.getCostFinal();
        // Use floor to stay close to an integer move count
        double moveCount = Math.floor(bestMoves);
        double totalCostApprox = fixed + moveCount * cap.getCostPerMove();
        double avgCostPerDelivery = totalCostApprox / numOfDeliveries;

        // Pro-rata cost must satisfy all maxCost constraints
        for (MedDispatchRec rec : dispatches) {
            var req = rec.getRequirements();
            Double maxCost = (req != null) ? req.getMaxCost() : null;
            if (maxCost != null && avgCostPerDelivery > maxCost) {
                return false;
            }
        }

        return true;
    }

    // From a service point entry,
    // collect drone IDs that can execute a given dispatch on a given day.
    public static List<String> feasibleDroneIdsAtSP(
            DroneForServicePoint spEntry,
            Map<String, Drone> droneById,
            MedDispatchRec rec,
            LocalDate day
    ) {
        if (spEntry == null || spEntry.getDrones() == null || droneById == null || rec == null)
            return List.of();
        List<String> res = new ArrayList<>();
        for (DroneForServicePoint.Item di : spEntry.getDrones()) {
            if (di == null) continue;
            Drone d = droneById.get(di.getId());
            if (d == null) continue;
            if (droneMeetsRec(d, di, rec, day)) {
                res.add(d.getId());
            }
        }
        return res;
    }

    // Check whether a specific drone (and its availability at a service point) satisfies a dispatch.
    public static boolean droneMeetsRec(
            Drone drone,
            DroneForServicePoint.Item atSP,
            MedDispatchRec rec,
            LocalDate day
    ) {
        if (drone == null || drone.getCapability() == null || rec == null || rec.getRequirements() == null)
            return false;

        var cap = drone.getCapability();
        var req = rec.getRequirements();
        if (req.isCooling() && !cap.isCooling()) return false;
        if (req.isHeating() && !cap.isHeating()) return false;
        if (req.getCapacity() > cap.getCapacity()) return false;

        LocalTime t = rec.getTime();
        List<DroneForServicePoint.Availability> windows =
                (atSP == null) ? null : atSP.getAvailability();

        return QueryDroneHelper.isAvailableAt(windows,day,t);
    }

    // Parse English day-of-week string into DayOfWeek
    private static DayOfWeek parseDayOfWeek(String s) {
        if (s == null) return null;
        try { return DayOfWeek.valueOf(s.toUpperCase(Locale.ROOT)); }
        catch (Exception e) { return null; }
    }

    // Parse a time string (HH:mm:ss) into  LocalTime
    private static LocalTime parseTimeSafe(String s) {
        if (s == null) return null;
        try { return LocalTime.parse(s); }
        catch (Exception e) { return null; }
    }
}