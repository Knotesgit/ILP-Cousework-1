package uk.ac.ed.acp.cw2.external;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.data.response.GeoJsonResponseCollection;
import uk.ac.ed.acp.cw2.utility.DeliveryPlanHelper;
import uk.ac.ed.acp.cw2.utility.DeliveryPlanner;
import uk.ac.ed.acp.cw2.utility.QueryDroneHelper;


import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DroneServiceImpl implements DroneService {
    private final IlpClientComponent ilpClient;

    public DroneServiceImpl(IlpClientComponent ilpClient) {
        this.ilpClient = ilpClient;
    }

    // Returns drone IDs filtered by cooling capability.
    @Override
    public List<String> dronesWithCooling(Boolean state){
        List<Drone> drones = ilpClient.getAllDrones();
        return drones.stream()
                .filter(d -> d.getCapability().isCooling() == state)
                .map(Drone::getId)
                .toList();
    }

    // Returns the drone matching the given ID or null if not found
    @Override
    public Drone droneDetails(String id){
        List<Drone> drones = ilpClient.getAllDrones();
        return drones.stream()
                .filter(d -> Objects.equals(d.getId(), id))
                .findFirst()
                .orElse(null);
    }

    // Returns IDs of drones whose given attribute matches the specified value
    @Override
    public List<String> queryAsPath(String attribute, String value){
        List<Drone> drones = ilpClient.getAllDrones();
        return drones.stream()
                .filter(d -> QueryDroneHelper.matches(d, attribute, value))
                .map(Drone::getId)
                .toList();
    }

    // Returns IDs of drones whose attributes' value matches the given query conditions
    @Override
    public List<String> query(List<QueryCondition> conditions){
        List<Drone> drones = ilpClient.getAllDrones();
        return drones.stream()
                .filter(d -> QueryDroneHelper.matchesConditions(d, conditions))
                .map(Drone::getId)
                .toList();
    }

    // Returns IDs of drones that are available for a list of medicine dispatch record
    @Override
    public List<String> queryAvailableDrones(List<MedDispatchRec> dispatches){
        if (dispatches == null || dispatches.isEmpty()) return List.of();
        for (MedDispatchRec rec : dispatches) {
            if (rec.getRequirements() == null) return List.of();
            if (rec.getRequirements().isCooling() && rec.getRequirements().isHeating())
                return List.of();
        }
        int n = dispatches.size();
        List<Drone> drones = ilpClient.getAllDrones();
        List<DroneForServicePoint> dfsp = ilpClient.getDronesForServicePoints();
        List<ServicePoint> servicePoints = ilpClient.getServicePoints();

        Map<String, List<DroneForServicePoint.Availability>> availability
                = QueryDroneHelper.buildAvailabilityIndex(dfsp);
        Map<String, List<ServicePoint>> homePoints = QueryDroneHelper.buildHomePointIndex(dfsp, servicePoints);

        return drones.stream()
                .filter(d -> QueryDroneHelper.canHandleAll(d,
                        availability.getOrDefault(d.getId(), Collections.emptyList()),
                        homePoints.getOrDefault(d.getId(), Collections.emptyList())
                        ,dispatches, n))
                .map(Drone::getId)
                .sorted()
                .toList();
    }


    @Override
    public CalcDeliveryPathResponse calcDeliveryPath(List<MedDispatchRec> recs) {
        // Basic record verification
        if(!DeliveryPlanHelper.isValidDispatchList(recs))
            return DeliveryPlanHelper.emptyDeliveryResponse();

        List<Drone> drones = ilpClient.getAllDrones();
        List<ServicePoint> servicePts = ilpClient.getServicePoints();
        List<RestrictedArea> areas = ilpClient.getRestrictedAreas();
        List<DroneForServicePoint> dfsp = ilpClient.getDronesForServicePoints();
        List<List<Coordinate>> restrictedPolys = DeliveryPlanHelper.extractPolygons(areas);
        List<BoundBox> BBoxes = DeliveryPlanHelper.extractBBoxes(areas);
        // Map drone by drone id
        Map<String, Drone> droneById = drones.stream().
                collect(Collectors.toMap(Drone::getId, d -> d));
        // Map serviceId to DroneForServicePoint
        Map<Integer, DroneForServicePoint> spMapDrone = dfsp.stream()
                .collect(Collectors.toMap
                        (DroneForServicePoint::getServicePointId, e -> e));

        List<MedDispatchRec> fixed = new ArrayList<>();
        List<MedDispatchRec> dateOnly = new ArrayList<>();
        List<MedDispatchRec> anytime = new ArrayList<>();
        for (MedDispatchRec r : recs) {
            if (r.getDate() != null && r.getTime() != null) fixed.add(r);
            else if (r.getDate() != null) dateOnly.add(r);
            else anytime.add(r);
        }
        Map<LocalDate, List<MedDispatchRec>> fixedByDate =
                fixed.stream().collect(Collectors.groupingBy(MedDispatchRec::getDate));
        Map<LocalDate, List<MedDispatchRec>> dateOnlyByDate =
                dateOnly.stream().collect(Collectors.groupingBy(MedDispatchRec::getDate));

        List<LocalDate> orderedDays = Stream.concat(
                        fixedByDate.keySet().stream(),
                        dateOnlyByDate.keySet().stream())
                .distinct().sorted().toList();

        // Group the dispatches by date and sorted in time sequence(compare id if same time)

        List<FlightBuilder> activeFlights = new ArrayList<>();
        List<FlightBuilder> finishedFlights = new ArrayList<>();

        for (LocalDate day : orderedDays) {
            List<MedDispatchRec> today = new ArrayList<>();
            var fx = new ArrayList<>(fixedByDate.getOrDefault(day, List.of()));
            fx.sort(Comparator.comparing(MedDispatchRec::getTime).thenComparing(MedDispatchRec::getId));
            today.addAll(fx);
            today.addAll(dateOnlyByDate.getOrDefault(day, List.of()));
            for (MedDispatchRec r : today) {
                if (!DeliveryPlanner.tryAssignOrStartFlight(servicePts, spMapDrone, droneById,
                        restrictedPolys, BBoxes, r, day, activeFlights, finishedFlights)) {
                    return DeliveryPlanHelper.emptyDeliveryResponse();
                }
            }

            for (Iterator<MedDispatchRec> it = anytime.iterator(); it.hasNext(); ) {
                MedDispatchRec r = it.next();
                boolean ok = DeliveryPlanner.tryAssignOrStartFlight(servicePts, spMapDrone, droneById,
                        restrictedPolys, BBoxes, r, day, activeFlights, finishedFlights);
                if (ok) it.remove();
            }
        }
        if(!activeFlights.isEmpty()){
            for(FlightBuilder fb : activeFlights)
                DeliveryPlanner.closeFlight(fb, finishedFlights, restrictedPolys, BBoxes);
        }
        return DeliveryPlanHelper.buildDeliveryResponse(finishedFlights);
    }

    @Override
    public GeoJsonResponseCollection calcDeliveryPathAsGeoJson(List<MedDispatchRec> recs) {
        return DeliveryPlanHelper.buildGeoJsonResponseCollection(calcDeliveryPath(recs));
    }
}
