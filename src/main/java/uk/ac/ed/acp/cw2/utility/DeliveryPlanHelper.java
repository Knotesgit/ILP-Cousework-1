package uk.ac.ed.acp.cw2.utility;

import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Helper utilities used by the delivery planning logic.
public class DeliveryPlanHelper {
    public DeliveryPlanHelper() {
    }

    // Create an empty CalcDeliveryPathResponse with no drone paths and zero cost/moves.
    public CalcDeliveryPathResponse emptyResponse() {
        CalcDeliveryPathResponse response = new CalcDeliveryPathResponse();
        response.setDronePaths(new ArrayList<>());  // empty list
        response.setTotalCost(0.0);
        response.setTotalMoves(0);
        return response;
    }

    // Aggregate a list of finished flights into a CalcDeliveryPathResponse
    public CalcDeliveryPathResponse buildResponse(List<FlightBuilder> finishedFlights) {
        CalcDeliveryPathResponse response = new CalcDeliveryPathResponse();
        double totalCost = 0.0;
        int totalMoves = 0;
        List<CalcDeliveryPathResponse.DronePath> dronePathsList = new ArrayList<>();

        if (finishedFlights == null || finishedFlights.isEmpty()) {
            return emptyResponse();
        }

        for (FlightBuilder fb : finishedFlights) {
            if (fb == null) continue;

            double cpm = fb.getCostPerMove();
            double ci  = fb.getCostInitial();
            double cf  = fb.getCostFinal();
            double flightCost = ci + cf + fb.getStepsUsed() * cpm;

            totalCost += flightCost;
            totalMoves += fb.getStepsUsed();

            CalcDeliveryPathResponse.DronePath dp = new CalcDeliveryPathResponse.DronePath();
            dp.setDroneId(fb.getDroneId());

            List<CalcDeliveryPathResponse.DeliverySegment> deliveriesList = new ArrayList<>();
            if (fb.getSegments() != null) {
                for (var seg : fb.getSegments()) {
                    if (seg == null) continue;
                    CalcDeliveryPathResponse.DeliverySegment out = new CalcDeliveryPathResponse.DeliverySegment();
                    out.setDeliveryId(seg.getDeliveryId());
                    out.setFlightPath(seg.getFlightPath() == null
                            ? new ArrayList<>()
                            : new ArrayList<>(seg.getFlightPath()));
                    deliveriesList.add(out);
                }
            }

            dp.setDeliveries(deliveriesList);
            dronePathsList.add(dp);
        }

        response.setTotalCost(totalCost);
        response.setTotalMoves(totalMoves);
        response.setDronePaths(dronePathsList);
        return response;
    }

    // Extract all polygon vertex lists from the given restricted areas.
    public List<List<Coordinate>> extractPolygons(List<RestrictedArea> areas) {
        List<List<Coordinate>> polygons = new ArrayList<>();
        if (areas == null) return polygons;
        for (RestrictedArea area : areas) {
            if (area == null) continue;
            List<Coordinate> vertices = area.getVertices();
            if (vertices != null && !vertices.isEmpty()) {
                // Add the polygon (list of vertices) to the result
                polygons.add(new ArrayList<>(vertices));
            }
        }
        return polygons;
    }

    // Compute bounding boxes (min/max lng/lat) for each restricted area polygon.
    public List<BoundBox> extractBBoxes(List<RestrictedArea> areas) {
        List<BoundBox> boxes = new ArrayList<>();
        if (areas == null) return boxes;
        for (RestrictedArea area : areas) {
            BoundBox BBox = polyBox(area.getVertices());
            boxes.add(BBox);
        }
        return boxes;
    }

    // From a service point entry,
    // collect drone IDs that can execute a given dispatch on a given day.
    public List<Integer> feasibleDroneIdsAtSP(
            DroneForServicePoint spEntry,
            Map<Integer, Drone> droneById,
            MedDispatchRec rec,
            LocalDate day
    ) {
        if (spEntry == null || spEntry.getDrones() == null || droneById == null || rec == null)
            return List.of();
        List<Integer> res = new ArrayList<>();
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
    public boolean droneMeetsRec(
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

        // Determine the effective day-of-week to check availability against
        DayOfWeek dow = null;
        if (day != null) dow = day.getDayOfWeek();
        else if (rec.getDate() != null) dow = rec.getDate().getDayOfWeek();

        LocalTime t = rec.getTime();
        List<DroneForServicePoint.Availability> windows =
                (atSP == null) ? null : atSP.getAvailability();

        // No date/time constraint
        if (dow == null && t == null) return true;

        if (windows == null || windows.isEmpty()) return false;
        if (dow != null && t == null) {
            for (var a : windows) {
                DayOfWeek wDow = parseDayOfWeek(a.getDayOfWeek());
                if (wDow != null && wDow == dow) return true;
            }
            return false;
        }

        // Day + time: time must fall inside a window on the same day
        for (var a : windows) {
            DayOfWeek wDow = parseDayOfWeek(a.getDayOfWeek());
            if (wDow == null || wDow != dow) continue;
            LocalTime from = parseTimeSafe(a.getFrom());
            LocalTime until = parseTimeSafe(a.getUntil());
            if (from != null && until != null && !t.isBefore(from) && !t.isAfter(until)) {
                return true;
            }
        }
        return false;
    }

    // Find the DroneForServicePoint.Item entry for a given drone ID in a service point.
    public DroneForServicePoint.Item findDroneItem(DroneForServicePoint dfsp, int droneId) {
        if (dfsp == null) return null;
        for (DroneForServicePoint.Item item : dfsp.getDrones()) {
            if (item.getId() == droneId) return item;
        }
        return null;
    }

    // Check that a per-delivery cost does not exceed any of the provided maxCost constraints.
    // Comparison uses a tolerance eps
    public boolean withinAllMaxCosts(double perDeliveryCost, List<Double> existingMaxCost, double eps) {
        if (existingMaxCost == null || existingMaxCost.isEmpty()) return true;
        for (Double cost : existingMaxCost) {
            if (cost == null) continue;
            if (perDeliveryCost - cost > eps) return false;
        }
        return true;
    }

    // Construct a bound box for a region
    private BoundBox polyBox(List<Coordinate> rectClosed) {
        double minLng = Double.POSITIVE_INFINITY, minLat = Double.POSITIVE_INFINITY;
        double maxLng = Double.NEGATIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        for (var c : rectClosed) {
            minLng = Math.min(minLng, c.getLng());
            maxLng = Math.max(maxLng, c.getLng());
            minLat = Math.min(minLat, c.getLat());
            maxLat = Math.max(maxLat, c.getLat());
        }
        return new BoundBox(new Coordinate(maxLng, maxLat), new Coordinate(minLng, minLat));
    }

    // Parse English day-of-week string into DayOfWeek
    private DayOfWeek parseDayOfWeek(String s) {
        if (s == null) return null;
        try { return DayOfWeek.valueOf(s.toUpperCase(Locale.ROOT)); }
        catch (Exception e) { return null; }
    }

    // Parse a time string (HH:mm:ss) into  LocalTime
    private LocalTime parseTimeSafe(String s) {
        if (s == null) return null;
        try { return LocalTime.parse(s); }
        catch (Exception e) { return null; }
    }
}