package uk.ac.ed.acp.cw2.utility;

import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;
import uk.ac.ed.acp.cw2.data.response.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Helper utilities used by the delivery planning logic.
public class DeliveryPlanHelper {

    // Check whether a list of MedDispatchRec valid or not
    public static boolean isValidDispatchList(List<MedDispatchRec> recs) {
        if (recs == null || recs.isEmpty()) return false;
        for (MedDispatchRec rec : recs) {
            if (rec == null) return false;
            if (rec.getId() == null) return false;
            if (rec.getRequirements() == null) return false;
            if (rec.getDelivery() == null) return false;
            if (rec.getDate() == null && rec.getTime() != null) return false;
            var req = rec.getRequirements();
            if (req.isCooling() && req.isHeating()) return false;
            if (req.getCapacity() == null) return false;
        }
        return true;
    }

    // Create an empty CalcDeliveryPathResponse with no drone paths and zero cost/moves.
    public static CalcDeliveryPathResponse emptyDeliveryResponse() {
        CalcDeliveryPathResponse response = new CalcDeliveryPathResponse();
        response.setDronePaths(new ArrayList<>());  // empty list
        response.setTotalCost(0.0);
        response.setTotalMoves(0);
        return response;
    }

    // Aggregate a list of finished flights into a CalcDeliveryPathResponse
    public static CalcDeliveryPathResponse buildDeliveryResponse(List<FlightBuilder> finishedFlights) {
        CalcDeliveryPathResponse response = new CalcDeliveryPathResponse();
        double totalCost = 0.0;
        int totalMoves = 0;
        List<CalcDeliveryPathResponse.DronePath> dronePathsList = new ArrayList<>();

        if (finishedFlights == null || finishedFlights.isEmpty()) {
            return emptyDeliveryResponse();
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

    // Build a GeoJSON FeatureCollection from CalcDeliveryPathResponse.
    public static GeoJsonResponseCollection buildGeoJsonResponseCollection(
            CalcDeliveryPathResponse resp) {

        GeoJsonResponseCollection collection = new GeoJsonResponseCollection();
        List<GeoJsonResponse> features = new ArrayList<>();

        if (resp == null || resp.getDronePaths() == null || resp.getDronePaths().isEmpty()) {
            collection.setFeatures(List.of());
            return collection;
        }

        for (CalcDeliveryPathResponse.DronePath dp : resp.getDronePaths()) {
            if (dp == null || dp.getDeliveries() == null || dp.getDeliveries().isEmpty())
                continue;

            List<Coordinate> merged = new ArrayList<>();
            Coordinate lastPrevCoord = null;

            // Merge all segments from this drone
            for (CalcDeliveryPathResponse.DeliverySegment seg : dp.getDeliveries()) {
                if (seg == null || seg.getFlightPath() == null || seg.getFlightPath().isEmpty())
                    continue;

                List<Coordinate> raw = seg.getFlightPath();
                List<Coordinate> cleaned;

                // Cross-segment dedupe: remove the first coordinate iff equal to previous segment’s last
                if (lastPrevCoord != null) {
                    Coordinate first = raw.get(0);
                    if (first.getLat() == lastPrevCoord.getLat() &&
                            first.getLng() == lastPrevCoord.getLng()) {
                        cleaned = raw.subList(1, raw.size());
                    } else {
                        cleaned = raw;
                    }
                } else {
                    cleaned = raw;
                }

                merged.addAll(cleaned);

                // Store end of this segment as candidate for dedupe
                lastPrevCoord = raw.get(raw.size() - 1);
            }

            // Convert to [[lng, lat]] list
            List<List<Double>> coords = toLngLat(merged);

            // Build Feature for this drone
            GeoJsonResponse feature = new GeoJsonResponse();

            GeoJsonResponse.Properties props = new GeoJsonResponse.Properties();
            props.setDroneId(dp.getDroneId());
            feature.setProperties(props);

            GeoJsonResponse.Geometry geom = new GeoJsonResponse.Geometry();
            geom.setCoordinates(coords);
            feature.setGeometry(geom);

            features.add(feature);
        }

        collection.setFeatures(features);
        return collection;
    }




    // Extract all polygon vertex lists from the given restricted areas.
    public static List<List<Coordinate>> extractPolygons(List<RestrictedArea> areas) {
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
    public static List<BoundBox> extractBBoxes(List<RestrictedArea> areas) {
        List<BoundBox> boxes = new ArrayList<>();
        if (areas == null) return boxes;
        for (RestrictedArea area : areas) {
            BoundBox BBox = polyBox(area.getVertices());
            boxes.add(BBox);
        }
        return boxes;
    }

    // Find the DroneForServicePoint.Item entry for a given drone ID in a service point.
    public static DroneForServicePoint.Item findDroneItem(DroneForServicePoint dfsp, String droneId) {
        if (dfsp == null) return null;
        for (DroneForServicePoint.Item item : dfsp.getDrones()) {
            if (Objects.equals(item.getId(), droneId)) return item;
        }
        return null;
    }

    // Check that a per-delivery cost does not exceed any of the provided maxCost constraints.
    // Comparison uses a tolerance eps
    public static boolean withinAllMaxCosts(double perDeliveryCost, List<Double> existingMaxCost, double eps) {
        if (existingMaxCost == null || existingMaxCost.isEmpty()) return true;
        for (Double cost : existingMaxCost) {
            if (cost == null) continue;
            if (perDeliveryCost - cost > eps) return false;
        }
        return true;
    }

    // Construct a bound box for a region
    private static BoundBox polyBox(List<Coordinate> rectClosed) {
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

    // Convert Coordinate to List of pairs of Lng lat
    private static List<List<Double>> toLngLat(List<Coordinate> line) {
        List<List<Double>> out = new ArrayList<>(line.size());
        for (Coordinate c : line) {
            if (c != null) out.add(List.of(c.getLng(), c.getLat()));
        }
        return out;
    }
}