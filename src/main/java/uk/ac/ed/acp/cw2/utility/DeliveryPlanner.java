package uk.ac.ed.acp.cw2.utility;

import uk.ac.ed.acp.cw2.data.*;

import java.time.LocalDate;
import java.util.*;

public class DeliveryPlanner {
    public DeliveryPlanner() {
    }

    public static boolean tryAssignOrStartFlight(
            List<ServicePoint> servicePts,
            Map<Integer, DroneForServicePoint> spMapDrone,
            Map<String, Drone> droneById,
            List<List<Coordinate>> restrictedPolys,
            List<BoundBox> boxes,
            MedDispatchRec r,
            LocalDate day,
            List<FlightBuilder> active,
            List<FlightBuilder> finished) {

        if (!active.isEmpty()) {
            boolean merged = tryMergeFlight(r, active, finished,
                    droneById, spMapDrone, restrictedPolys, boxes, day);
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

    public static FlightBuilder openNewFlight(
            List<ServicePoint> spCandidates,
            Map<String, Drone> droneById,
            Map<Integer, DroneForServicePoint> spMapDrone,
            MedDispatchRec rec, List<List<Coordinate>> restrictedPolys,
            List<BoundBox> boxes, LocalDate day) {
        Coordinate target = rec.getDelivery();
        if (target == null) return null;
        spCandidates.sort(Comparator.comparingDouble(
                sp -> GeoUtilities.distanceBetween(sp.getLocation(), target)));
        for (ServicePoint sp : spCandidates) {
            List<String> availableDroneIds = QueryDroneHelper.
                    feasibleDroneIdsAtSP(spMapDrone.get(sp.getId()),
                            droneById, rec, day);
            if (availableDroneIds.isEmpty())
                continue;
            List<Coordinate> forward = GeoUtilities.pathBetween(
                    sp.getLocation(), target, restrictedPolys, boxes);
            if (forward.isEmpty()) continue;
            int fSteps = forward.size() - 1;
            List<Coordinate> forwardWithHover = new ArrayList<Coordinate>(forward);
            forwardWithHover.add(forward.getLast());
            // Same path to return if only one delivery
            // +1 for hover
            int neededStepsNow = fSteps + fSteps + 1;
            String bestDroneId = null;
            double bestEstCost = (rec.getRequirements().getMaxCost() == null) ?
                    Double.MAX_VALUE : rec.getRequirements().getMaxCost();
            for (String id : availableDroneIds) {
                Drone d = droneById.get(id);
                var cap = d.getCapability();
                if (neededStepsNow > cap.getMaxMoves()) continue;
                // estimation of cost and steps
                double est = cap.getCostInitial() + cap.getCostFinal()
                        + cap.getCostPerMove() * neededStepsNow;
                if (est < bestEstCost) {
                    bestEstCost = est;
                    bestDroneId = d.getId();
                }
            }
            if (bestDroneId == null) continue;

            Drone d = droneById.get(bestDroneId);
            var cap = d.getCapability();
            FlightBuilder fb = new FlightBuilder(
                    d.getId(), sp,
                    cap.getCapacity(),
                    cap.getMaxMoves(),
                    cap.getCostPerMove(),
                    cap.getCostInitial(), cap.getCostFinal(), rec);
            fb.addSegment(
                    rec.getId(),
                    forwardWithHover,
                    (fSteps + 1),
                    rec.getRequirements().getCapacity(),
                    rec.getRequirements().getMaxCost(),
                    rec.getRequirements().isCooling(),
                    rec.getRequirements().isHeating()
            );
            return fb;
        }
        return null;
    }

    public static boolean tryMergeFlight(MedDispatchRec rec,
                                  List<FlightBuilder> actives, List<FlightBuilder> finished,
                                  Map<String, Drone> droneById,
                                  Map<Integer, DroneForServicePoint> spMapDrone,
                                  List<List<Coordinate>> restrictedPolys,
                                  List<BoundBox> boxes,
                                  LocalDate day) {

        if (actives == null || actives.isEmpty()) {
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
            // Time availability check and cooling/heating functionality check
            var droneItem = DeliveryPlanHelper.findDroneItem(
                    spMapDrone.get(fb.getServicePoint().getId()), fb.getDroneId());
            if (!QueryDroneHelper.droneMeetsRec(droneById.get(fb.getDroneId()),
                    droneItem, rec, day))
                continue;

            // Capacity check
            if (rec.getRequirements().getCapacity() + fb.getCurrentLoad() > fb.getCapacity())
                continue;
            // Max Step Check
            List<Coordinate> forward = GeoUtilities.pathBetween(fb.getEnd(), rec.getDelivery(),
                    restrictedPolys, boxes);
            if (forward.isEmpty()) continue;
            List<Coordinate> forwardWithHover = new ArrayList<Coordinate>(forward);
            forwardWithHover.add(forward.getLast());
            int fSteps = forwardWithHover.size() - 1;
            // Early termination
            if (fSteps + fb.getStepsUsed() > fb.getMaxMoves()) {
                continue;
            }
            // PreCheck return step and cost
            List<Coordinate> back = GeoUtilities.pathBetween(rec.getDelivery(),
                    fb.getServicePoint().getLocation(), restrictedPolys, boxes);
            if (back.isEmpty()) continue;
            int bSteps = back.size() - 1;
            if (fSteps + bSteps + fb.getStepsUsed() > fb.getMaxMoves()) {
                continue;
            }
            Double maxCost = rec.getRequirements().getMaxCost();
            if (maxCost != null) {
                int deliveryCount = fb.getDeliveryCount() + 1;
                List<Double> existingMaxCost = new ArrayList<Double>(fb.getExistingMaxCosts());
                existingMaxCost.add(maxCost);
                double estForEachDelivery = ((fb.getStepsUsed() + fSteps + bSteps) * fb.getCostPerMove() +
                        fb.getCostInitial() + fb.getCostFinal()) / deliveryCount;
                if (!DeliveryPlanHelper.withinAllMaxCosts(estForEachDelivery, existingMaxCost, 1e-12))
                    continue;
            }
            var req = rec.getRequirements();
            fb.addSegment(rec.getId(), forwardWithHover, fSteps, req.getCapacity(),
                    req.getMaxCost(), req.isCooling(), req.isHeating());
            return true;
        }
        return false;
    }

    public static void closeFlight(FlightBuilder fb, List<FlightBuilder> finished,
                            List<List<Coordinate>> restrictedPolys, List<BoundBox> boxes) {
        // Same path to return if only one delivery
        // remove reversed first for hover
        if (fb.getDeliveryCount() == 1) {
            List<Coordinate> forwardWithHover = fb.getSegments().getFirst().getFlightPath();
            List<Coordinate> back = new ArrayList<Coordinate>(forwardWithHover);
            back.removeLast();
            back = back.reversed();
            fb.addReturn(back, (back.size() - 1));
            finished.add(fb);
        } else {
            List<Coordinate> back = GeoUtilities.pathBetween(fb.getEnd(),
                    fb.getServicePoint().getLocation(), restrictedPolys, boxes);
            fb.addReturn(back, (back.size() - 1));
            finished.add(fb);
        }
    }
}