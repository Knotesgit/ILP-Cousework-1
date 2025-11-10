package uk.ac.ed.acp.cw2.external;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.data.response.GeoJsonResponse;
import uk.ac.ed.acp.cw2.utility.DeliveryPlanHelper;
import uk.ac.ed.acp.cw2.utility.QueryDroneHelper;
import uk.ac.ed.acp.cw2.service.GeoService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DroneServiceImpl implements DroneService {
    private final IlpClientComponent ilpClient;
    private final GeoService geo;
    private final QueryDroneHelper QueryDroneHelper = new QueryDroneHelper();
    private final DeliveryPlanHelper deliveryPlanHelper = new DeliveryPlanHelper();

    public DroneServiceImpl(IlpClientComponent ilpClient,GeoService geo) {
        this.ilpClient = ilpClient;
        this.geo = geo;
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
                .filter(d -> QueryDroneHelper.matches(d, attribute, value))
                .map(Drone::getId)
                .toList();
    }

    // Returns IDs of drones whose attributes' value matches the given query conditions
    @Override
    public List<Integer> queryByAttributes(List<QueryCondition> conditions){
        List<Drone> drones = ilpClient.getAllDrones();
        return drones.stream()
                .filter(d -> QueryDroneHelper.matchesConditions(d, conditions))
                .map(Drone::getId)
                .toList();
    }

    // Returns IDs of drones that are available for a list of medicine dispatch record
    @Override
    public List<Integer> queryAvailableDrones(List<MedDispatchRec> dispatches){
        if (dispatches == null || dispatches.isEmpty()) return List.of();
        for (MedDispatchRec rec : dispatches) {
            if (rec.getRequirements() == null) return List.of();
            if (rec.getRequirements().isCooling() && rec.getRequirements().isHeating())
                return List.of();
        }
        int n = dispatches.size();
        List<Drone> drones = ilpClient.getAllDrones();
        List<DroneForServicePoint> dfsp = ilpClient.getDronesForServicePoints();

        Map<Integer, List<DroneForServicePoint.Availability>> availability = QueryDroneHelper.buildAvailabilityIndex(dfsp);

        return drones.stream()
                .filter(d -> QueryDroneHelper.canHandleAll(d, availability.getOrDefault(d.getId(),
                        Collections.emptyList()), dispatches, n))
                .map(Drone::getId)
                .sorted()
                .toList();
    }


    @Override
    public CalcDeliveryPathResponse calcDeliveryPath(List<MedDispatchRec> recs) {
        // Basic record verification
        if(!deliveryPlanHelper.isValidDispatchList(recs))
            return deliveryPlanHelper.emptyDeliveryResponse();

        List<Drone> drones = ilpClient.getAllDrones();
        List<ServicePoint> servicePts = ilpClient.getServicePoints();
        List<RestrictedArea> areas = ilpClient.getRestrictedAreas();
        List<DroneForServicePoint> dfsp = ilpClient.getDronesForServicePoints();
        List<List<Coordinate>> restrictedPolys = deliveryPlanHelper.extractPolygons(areas);
        List<BoundBox> BBoxes = deliveryPlanHelper.extractBBoxes(areas);
        // Map drone by drone id
        Map<Integer, Drone> droneById = drones.stream().
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
                if (!tryAssignOrStartFlight(servicePts, spMapDrone, droneById,
                        restrictedPolys, BBoxes, r, day, activeFlights, finishedFlights)) {
                    return deliveryPlanHelper.emptyDeliveryResponse();
                }
            }

            for (Iterator<MedDispatchRec> it = anytime.iterator(); it.hasNext(); ) {
                MedDispatchRec r = it.next();
                boolean ok = tryAssignOrStartFlight(servicePts, spMapDrone, droneById,
                        restrictedPolys, BBoxes, r, day, activeFlights, finishedFlights);
                if (ok) it.remove();
            }
        }
        if(!activeFlights.isEmpty()){
            for(FlightBuilder fb : activeFlights)
                closeFlight(fb,finishedFlights,restrictedPolys,BBoxes);
        }
        return deliveryPlanHelper.buildDeliveryResponse(finishedFlights);
    }

    @Override
    public GeoJsonResponse calcDeliveryPathAsGeoJson(List<MedDispatchRec> recs){
        // Basic record verification
        if(!deliveryPlanHelper.isValidDispatchList(recs))
            return deliveryPlanHelper.emptyGeoJsonResponse();

        // Fast termination if not Completable by single drone(different date)
        LocalDate date = recs.get(0).getDate();
        for (MedDispatchRec rec : recs)
            if(!rec.getDate().equals(date))
                return deliveryPlanHelper.emptyGeoJsonResponse();

        List<Drone> drones = ilpClient.getAllDrones();
        List<ServicePoint> servicePts = ilpClient.getServicePoints();
        List<RestrictedArea> areas = ilpClient.getRestrictedAreas();
        List<DroneForServicePoint> dfsp = ilpClient.getDronesForServicePoints();
        List<List<Coordinate>> restrictedPolys = deliveryPlanHelper.extractPolygons(areas);
        List<BoundBox> BBoxes = deliveryPlanHelper.extractBBoxes(areas);
        // Map drone by drone id
        Map<Integer, Drone> droneById = drones.stream().
                collect(Collectors.toMap(Drone::getId, d -> d));
        // Map serviceId to DroneForServicePoint
        Map<Integer, DroneForServicePoint> spMapDrone = dfsp.stream()
                .collect(Collectors.toMap
                        (DroneForServicePoint::getServicePointId, e -> e));

        List<Integer> availableDrones = queryAvailableDrones(recs);
        if(availableDrones == null || availableDrones.isEmpty())
            return deliveryPlanHelper.emptyGeoJsonResponse();
        Set<Integer> allow = new HashSet<>(availableDrones);
        droneById.keySet().retainAll(allow);
        if (droneById.isEmpty()) return deliveryPlanHelper.emptyGeoJsonResponse();

        // Dispatch in time order.
        recs.sort(Comparator.comparing(MedDispatchRec::getTime).thenComparing(MedDispatchRec::getId));

        FlightBuilder fb = openNewFlight(
                servicePts, droneById, spMapDrone,
                recs.get(0), restrictedPolys, BBoxes, recs.get(0).getDate()
        );

        if (fb == null) return deliveryPlanHelper.emptyGeoJsonResponse();
        List<FlightBuilder> activeFlights = new ArrayList<>();
        List<FlightBuilder> finishedFlights = new ArrayList<>();
        activeFlights.add(fb);

        boolean ok = true;
        for (int i = 1; i < recs.size(); i++) {
            boolean merged = tryMergeFlight(
                    recs.get(i), activeFlights, finishedFlights,
                    droneById, spMapDrone, restrictedPolys, BBoxes,
                    recs.get(i).getDate()
            );
            if (!merged) {
                ok = false;
                break; }
        }

        if (!ok)
            return deliveryPlanHelper.emptyGeoJsonResponse();
        if (activeFlights.size() != 1)
            return deliveryPlanHelper.emptyGeoJsonResponse();
        closeFlight(activeFlights.get(0), finishedFlights, restrictedPolys, BBoxes);
        if (finishedFlights.size() != 1 ||
                finishedFlights.get(0).getDeliveryCount() != recs.size())
            return deliveryPlanHelper.emptyGeoJsonResponse();
        return deliveryPlanHelper.buildGeoJsonResponse(finishedFlights.get(0));
    }

    private boolean tryAssignOrStartFlight(
            List<ServicePoint> servicePts,
            Map<Integer, DroneForServicePoint> spMapDrone,
            Map<Integer, Drone> droneById,
            List<List<Coordinate>> restrictedPolys,
            List<BoundBox> boxes,
            MedDispatchRec r,
            LocalDate day,
            List<FlightBuilder> active,
            List<FlightBuilder> finished) {

        if (!active.isEmpty()) {
            boolean merged = tryMergeFlight(r,active, finished,
                    droneById,spMapDrone, restrictedPolys, boxes, day);
            if (merged) return true;
        }

        FlightBuilder fb = openNewFlight(servicePts, droneById, spMapDrone, r,
                restrictedPolys, boxes, day);
        if (fb != null) {
            active.add(fb);
            return true;
        }
        return false;
    }

    private FlightBuilder openNewFlight(
            List<ServicePoint> spCandidates,
            Map<Integer, Drone> droneById,
            Map<Integer, DroneForServicePoint> spMapDrone,
            MedDispatchRec rec, List<List<Coordinate>> restrictedPolys,
            List<BoundBox> boxes,LocalDate day){
        Coordinate target = rec.getDelivery();
        if (target == null) return null;
        spCandidates.sort(Comparator.comparingDouble(
                sp -> geo.distanceBetween(sp.getLocation(), target)));
                for (ServicePoint sp:spCandidates){
                    List<Integer> availableDroneIds = deliveryPlanHelper.
                            feasibleDroneIdsAtSP(spMapDrone.get(sp.getId()),
                                    droneById, rec, day);
                    if(availableDroneIds.isEmpty())
                        continue;
                    List<Coordinate> forward = geo.pathBetween(
                            sp.getLocation(), target, restrictedPolys, boxes);
                    if(forward.isEmpty()) continue;
                    int fSteps = forward.size() - 1;
                    List<Coordinate> forwardWithHover = new ArrayList<>(forward);
                    forwardWithHover.add(forward.getLast());
                    // Same path to return if only one delivery
                    // +1 for hover
                    int neededStepsNow = fSteps + fSteps + 1;
                    int bestDroneId = -1;
                    double bestEstCost = (rec.getRequirements().getMaxCost()==null) ?
                            Double.MAX_VALUE : rec.getRequirements().getMaxCost();
                    for(Integer id : availableDroneIds){
                        Drone d = droneById.get(id);
                        var cap = d.getCapability();
                        if(neededStepsNow > cap.getMaxMoves()) continue;
                        // estimation of cost and steps
                        double est = cap.getCostInitial()+cap.getCostFinal()
                                +cap.getCostPerMove()*neededStepsNow;
                        if(est < bestEstCost){
                            bestEstCost = est;
                            bestDroneId = d.getId();
                        }
                    }
                    if(bestDroneId < 0) continue;

                    Drone d = droneById.get(bestDroneId);
                    var cap =  d.getCapability();
                    FlightBuilder fb = new FlightBuilder(
                            d.getId(), sp,
                            cap.getCapacity(),
                            cap.getMaxMoves(),
                            cap.getCostPerMove(),
                            cap.getCostInitial(), cap.getCostFinal(),rec);
                    fb.addSegment(
                            rec.getId(),
                            forwardWithHover,
                            (fSteps+1),
                            rec.getRequirements().getCapacity(),
                            rec.getRequirements().getMaxCost(),
                            rec.getRequirements().isCooling(),
                            rec.getRequirements().isHeating()
                    );
                    return fb;
                }
                return null;
    }
    private boolean tryMergeFlight(MedDispatchRec rec,
                                   List<FlightBuilder> actives,List<FlightBuilder> finished,
                                   Map<Integer, Drone> droneById,
                                   Map<Integer, DroneForServicePoint> spMapDrone,
                                   List<List<Coordinate>> restrictedPolys,
                                   List<BoundBox> boxes,
                                   LocalDate day) {

        if(actives == null || actives.isEmpty()){
            return false;
        }
        ListIterator<FlightBuilder> it = actives.listIterator();
        while (it.hasNext()) {
            // Time availability Check
            FlightBuilder fb = it.next();
            LocalDate fd = fb.getFlightDate();
            if (fd != null && !fd.equals(day)) {
                it.remove();
                closeFlight(fb, finished, restrictedPolys, boxes);
                continue;
            }
            var droneItem = deliveryPlanHelper.findDroneItem(
                    spMapDrone.get(fb.getServicePoint().getId()),fb.getDroneId());
            if(!deliveryPlanHelper.droneMeetsRec(droneById.get(fb.getDroneId()),
                    droneItem,rec,day))
                continue;

            // Can't bring medicine that require both heating and cooling
            if(rec.getRequirements().isCooling())
                if (fb.isHasHeating())
                    continue;
            if (rec.getRequirements().isHeating())
                if (fb.isHasCooling())
                    continue;
            // Capacity check
            if(rec.getRequirements().getCapacity()+fb.getCurrentLoad()>fb.getCapacity())
                continue;
            // Max Step Check
            List<Coordinate> forward =  geo.pathBetween(fb.getEnd(),rec.getDelivery(),
                    restrictedPolys,boxes);
            if (forward.isEmpty()) continue;
            List<Coordinate> forwardWithHover = new ArrayList<>(forward);
            forwardWithHover.add(forward.getLast());
            int fSteps = forwardWithHover.size() - 1;
            // Early termination
            if (fSteps + fb.getStepsUsed() > fb.getMaxMoves()){
                continue;
            }
            // PreCheck return step and cost
            List<Coordinate> back = geo.pathBetween(rec.getDelivery(),
                    fb.getServicePoint().getLocation(),restrictedPolys,boxes);
            if (back.isEmpty()) continue;
            int bSteps =  back.size() - 1;
            if (fSteps + bSteps + fb.getStepsUsed() > fb.getMaxMoves()){
                continue;
            }
            Double maxCost= rec.getRequirements().getMaxCost();
            if(maxCost !=null){
                int deliveryCount = fb.getDeliveryCount()+1;
                List<Double> existingMaxCost = new ArrayList<>(fb.getExistingMaxCosts());
                existingMaxCost.add(maxCost);
                double estForEachDelivery = ((fb.getStepsUsed()+ fSteps + bSteps)*fb.getCostPerMove() +
                        fb.getCostInitial() + fb.getCostFinal())/deliveryCount;
                if(!deliveryPlanHelper.withinAllMaxCosts(estForEachDelivery,existingMaxCost,1e-12))
                    continue;
            }
            var req = rec.getRequirements();
            fb.addSegment(rec.getId(),forwardWithHover,fSteps,req.getCapacity(),
                    req.getMaxCost(),req.isCooling(),req.isHeating());
            return true;
        }
        return false;
    }

    private void closeFlight(FlightBuilder fb,List<FlightBuilder> finished,
                                List<List<Coordinate>> restrictedPolys, List<BoundBox> boxes){
        // Same path to return if only one delivery
        // remove reversed first for hover
        if(fb.getDeliveryCount() == 1){
            List<Coordinate> forwardWithHover = fb.getSegments().getFirst().getFlightPath();
            List<Coordinate> back = new ArrayList<>(forwardWithHover);
            back.removeLast();
            back = back.reversed();
            fb.addReturn(back,(back.size()-1));
            finished.add(fb);
        }
        else{
            List<Coordinate> back = geo.pathBetween(fb.getEnd(),
                fb.getServicePoint().getLocation(), restrictedPolys, boxes);
            fb.addReturn(back,(back.size()-1));
            finished.add(fb);
        }
    }
}
