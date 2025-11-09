package uk.ac.ed.acp.cw2.utility;

import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class QueryDroneHelper {
    public QueryDroneHelper() {
    }

    // Helper method to match a drone against a specific attribute and value
    public boolean matches(Drone d, String attr, String val) {
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
    public boolean matchesConditions(Drone d, List<QueryCondition> conditions) {
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
    public boolean compareNumeric(Drone d, String attr, String op, String val) {
        try {
            double droneVal = switch (attr.toLowerCase()) {
                case "id" -> d.getId();
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

    // Builds an index of availability windows: droneId -> DayOfWeek -> list of [from, until] time windows.
    public static Map<Integer, List<DroneForServicePoint.Availability>>
    buildAvailabilityIndex(List<DroneForServicePoint> dfspList) {
        Map<Integer, List<DroneForServicePoint.Availability>> map = new HashMap<>();
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

    // Checks if a drone can fulfill all dispatches
    // given capacity/heating/cooling, cost bound, and availability windows.
    public boolean canHandleAll(Drone drone, List<DroneForServicePoint.Availability> windows,
                                List<MedDispatchRec> dispatches, int numOfDeliveries) {
        if (drone == null || drone.getCapability() == null) return false;
        var cap = drone.getCapability();
        double totalRequired = 0.0;
        for (MedDispatchRec rec : dispatches) {
            var req = rec.getRequirements();
            if (req == null || req.getCapacity() == null) return false;
            if (cap.getCapacity() < req.getCapacity()) return false;
            totalRequired += req.getCapacity();
            if (req.isCooling() && !cap.isCooling()) return false;
            if (req.isHeating() && !cap.isHeating()) return false;

            if (req.getMaxCost() != null) {
                double fixed = cap.getCostInitial() + cap.getCostFinal();
                if (fixed / numOfDeliveries > req.getMaxCost()) return false;
            }
            if (!isAvailableAt(windows, rec.getDate(), rec.getTime())) return false;
        }
        if (totalRequired > cap.getCapacity()) return false;
        return true;
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