package uk.ac.ed.acp.cw2.integration;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Drone;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;
import uk.ac.ed.acp.cw2.data.MedDispatchRec;
import uk.ac.ed.acp.cw2.data.ServicePoint;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.external.DroneServiceImpl;
import uk.ac.ed.acp.cw2.external.IlpClientComponent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for calcDeliveryPath using the real IlpClientComponent,
 * which calls the remote ILP REST service over HTTP.
 *
 * These tests are NOT unit tests: they are slow, hit the network, and depend
 * on the external ILP service being available and stable.
 */
class CalcDeliveryPathIntegrationTests {

    /**
     * Resolve the ILP endpoint in exactly the same way as IlpRestServiceConfig.
     */
    private String resolveEndpoint() {
        String endpoint = System.getenv("ILP_ENDPOINT");
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/";
        }
        return endpoint;
    }

    /**
     * Create a real IlpClientComponent that talks to the ILP REST service.
     */
    private IlpClientComponent createRealIlpClient() {
        return new IlpClientComponent(resolveEndpoint());
    }

    private MedDispatchRec.Requirement requirementFromDrone(Drone d) {
        var cap = d.getCapability();
        assertNotNull(cap, "Real drone has null capability: " + d.getId());

        MedDispatchRec.Requirement r = new MedDispatchRec.Requirement();

        double droneCap = cap.getCapacity();
        // Be conservative: use at most 1.0 or the drone's capacity
        r.setCapacity(Math.min(1.0, droneCap));

        // Do not require cooling/heating to avoid over-constraining the match
        r.setCooling(false);
        r.setHeating(false);

        r.setMaxCost(null);
        return r;
    }

    private MedDispatchRec dispatch(
            int id,
            LocalDate date,
            LocalTime time,
            Coordinate delivery,
            MedDispatchRec.Requirement req
    ) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(id);
        rec.setDate(date);
        rec.setTime(time);
        rec.setDelivery(delivery);
        rec.setRequirements(req);
        return rec;
    }

    /**
     * Simple holder for a (Drone, ServicePoint) pair that is available
     * on a given day of week according to the real ILP data.
     */
    private static class AvailablePair {
        final Drone drone;
        final ServicePoint servicePoint;

        AvailablePair(Drone drone, ServicePoint servicePoint) {
            this.drone = drone;
            this.servicePoint = servicePoint;
        }
    }

    /**
     * Find a (Drone, ServicePoint) pair that is available on the given dayOfWeek
     * using the real drones, servicePoints, and drones-for-service-points data.
     */
    private AvailablePair findAvailablePairForDay(
            List<Drone> drones,
            List<ServicePoint> servicePoints,
            List<DroneForServicePoint> dfsps,
            String dayOfWeek // "MONDAY", "TUESDAY", ...
    ) {
        Map<String, Drone> droneById = drones.stream()
                .collect(Collectors.toMap(Drone::getId, d -> d));

        Map<Integer, ServicePoint> spById = servicePoints.stream()
                .collect(Collectors.toMap(ServicePoint::getId, sp -> sp));

        for (DroneForServicePoint dfsp : dfsps) {
            ServicePoint sp = spById.get(dfsp.getServicePointId());
            if (sp == null || sp.getLocation() == null) continue;

            List<DroneForServicePoint.Item> items = dfsp.getDrones();
            if (items == null) continue;

            for (DroneForServicePoint.Item item : items) {
                List<DroneForServicePoint.Availability> avs = item.getAvailability();
                if (avs == null) continue;

                boolean hasDay = avs.stream()
                        .anyMatch(a -> dayOfWeek.equalsIgnoreCase(a.getDayOfWeek()));

                if (!hasDay) continue;

                Drone drone = droneById.get(item.getId());
                if (drone == null) continue;

                return new AvailablePair(drone, sp);
            }
        }

        throw new IllegalStateException("No drone/servicePoint pair found for day " + dayOfWeek);
    }

    /**
     * Helper structure for locating a deliveryId inside the response:
     * which dronePath it belongs to, and at which segment index.
     */
    private static class DeliveryLocation {
        final int pathIndex;
        final int segmentIndex;

        DeliveryLocation(int pathIndex, int segmentIndex) {
            this.pathIndex = pathIndex;
            this.segmentIndex = segmentIndex;
        }
    }

    /**
     * Find the first occurrence of a given deliveryId in the response.
     * Returns null if not found.
     */
    private DeliveryLocation findDeliveryLocation(
            CalcDeliveryPathResponse res,
            int deliveryId
    ) {
        List<CalcDeliveryPathResponse.DronePath> paths = res.getDronePaths();
        if (paths == null) {
            return null;
        }
        for (int p = 0; p < paths.size(); p++) {
            List<CalcDeliveryPathResponse.DeliverySegment> segs = paths.get(p).getDeliveries();
            if (segs == null) continue;
            for (int s = 0; s < segs.size(); s++) {
                Integer id = segs.get(s).getDeliveryId();
                if (id != null && id == deliveryId) {
                    return new DeliveryLocation(p, s);
                }
            }
        }
        return null;
    }

    // ----------------------------------------------------------------------
    // Integration Test 1: Single dispatch with real ILP data
    // ----------------------------------------------------------------------

    @Test
    void singleDispatch_withRealIlpData_buildsAFeasibleFlight() {
        IlpClientComponent ilpClient = createRealIlpClient();
        DroneServiceImpl service = new DroneServiceImpl(ilpClient);

        List<Drone> drones = ilpClient.getAllDrones();
        assertNotNull(drones);
        assertFalse(drones.isEmpty(), "Real ILP data contains no drones");

        List<ServicePoint> sps = ilpClient.getServicePoints();
        assertNotNull(sps);
        assertFalse(sps.isEmpty(), "Real ILP data contains no service points");

        List<DroneForServicePoint> dfsps = ilpClient.getDronesForServicePoints();
        assertNotNull(dfsps);
        assertFalse(dfsps.isEmpty(), "Real ILP data contains no drones-for-service-points");

        LocalDate date = LocalDate.of(2025, 1, 6); // Monday
        String dow = date.getDayOfWeek().toString(); // "MONDAY"

        AvailablePair pair = findAvailablePairForDay(drones, sps, dfsps, dow);
        Drone chosenDrone = pair.drone;
        ServicePoint baseSp = pair.servicePoint;
        assertNotNull(chosenDrone.getCapability(), "Chosen drone has null capability");
        assertNotNull(baseSp.getLocation(), "Chosen service point has null location");

        MedDispatchRec.Requirement req = requirementFromDrone(chosenDrone);
        Coordinate delivery = baseSp.getLocation();

        MedDispatchRec rec = dispatch(
                9001,
                date,
                LocalTime.of(10, 0),
                delivery,
                req
        );

        CalcDeliveryPathResponse res = service.calcDeliveryPath(List.of(rec));

        assertNotNull(res, "Response must not be null");
        assertNotNull(res.getDronePaths(), "Drone paths must not be null");

        assertFalse(res.getDronePaths().isEmpty(), "Expected at least one drone path");

        CalcDeliveryPathResponse.DronePath dp = res.getDronePaths().getFirst();
        assertNotNull(dp.getDroneId());
        assertFalse(dp.getDroneId().isBlank(), "Drone id should not be blank");

        List<CalcDeliveryPathResponse.DeliverySegment> segs = dp.getDeliveries();
        assertNotNull(segs);
        assertFalse(segs.isEmpty(), "Expected at least one delivery segment");

        boolean hasOurDelivery = segs.stream()
                .anyMatch(seg -> rec.getId().equals(seg.getDeliveryId()));
        assertTrue(hasOurDelivery, "Expected a segment with deliveryId=" + rec.getId());

        for (CalcDeliveryPathResponse.DeliverySegment seg : segs) {
            assertNotNull(seg.getFlightPath(), "Flight path must not be null");
            assertFalse(seg.getFlightPath().isEmpty(), "Flight path must not be empty");
        }

        assertTrue(res.getTotalMoves() >= 0, "Total moves should be >= 0");
        assertTrue(res.getTotalCost() >= 0.0, "Total cost should be >= 0");
    }

    // ----------------------------------------------------------------------
    // Integration Test 2: Two dispatches on the same day
    // ----------------------------------------------------------------------

    @Test
    void multipleDispatches_sameDay_allDeliveriesPresent() {
        IlpClientComponent ilpClient = createRealIlpClient();
        DroneServiceImpl service = new DroneServiceImpl(ilpClient);

        List<Drone> drones = ilpClient.getAllDrones();
        assertNotNull(drones);
        assertFalse(drones.isEmpty(), "Real ILP data contains no drones");

        List<ServicePoint> sps = ilpClient.getServicePoints();
        assertNotNull(sps);
        assertFalse(sps.isEmpty(), "Real ILP data contains no service points");

        List<DroneForServicePoint> dfsps = ilpClient.getDronesForServicePoints();
        assertNotNull(dfsps);
        assertFalse(dfsps.isEmpty(), "Real ILP data contains no drones-for-service-points");

        LocalDate date = LocalDate.of(2025, 1, 6); // Monday
        String dow = date.getDayOfWeek().toString(); // "MONDAY"

        AvailablePair pair = findAvailablePairForDay(drones, sps, dfsps, dow);
        Drone chosenDrone = pair.drone;
        ServicePoint baseSp = pair.servicePoint;
        assertNotNull(chosenDrone.getCapability(), "Chosen drone has null capability");
        assertNotNull(baseSp.getLocation(), "Chosen service point has null location");

        MedDispatchRec.Requirement req = requirementFromDrone(chosenDrone);
        Coordinate delivery = baseSp.getLocation();

        // Two dispatches on the same day, different times
        MedDispatchRec early = dispatch(
                9101,
                date,
                LocalTime.of(10, 0),
                delivery,
                req
        );
        MedDispatchRec late = dispatch(
                9102,
                date,
                LocalTime.of(11, 0),
                delivery,
                req
        );

        // Intentionally pass in [late, early] to see if calcDeliveryPath
        // internally respects date/time ordering when building flights.
        CalcDeliveryPathResponse res = service.calcDeliveryPath(List.of(late, early));

        assertNotNull(res, "Response must not be null");
        assertNotNull(res.getDronePaths(), "Drone paths must not be null");
        assertFalse(res.getDronePaths().isEmpty(), "Expected at least one drone path");

        DeliveryLocation locEarly = findDeliveryLocation(res, early.getId());
        DeliveryLocation locLate = findDeliveryLocation(res, late.getId());

        assertNotNull(locEarly, "Expected deliveryId=" + early.getId() + " to appear in the plan");
        assertNotNull(locLate, "Expected deliveryId=" + late.getId() + " to appear in the plan");

        // If both deliveries are handled by the same drone path,
        // the earlier delivery should not appear after the later one.
        if (locEarly.pathIndex == locLate.pathIndex) {
            assertTrue(
                    locEarly.segmentIndex <= locLate.segmentIndex,
                    "Early delivery should not appear after later delivery in the same path"
            );
        }
    }

    // ----------------------------------------------------------------------
    // Integration Test 3: Impossible capacity -> empty plan
    // ----------------------------------------------------------------------

    @Test
    void dispatchWithImpossibleCapacity_yieldsEmptyPlan() {
        IlpClientComponent ilpClient = createRealIlpClient();
        DroneServiceImpl service = new DroneServiceImpl(ilpClient);

        List<ServicePoint> sps = ilpClient.getServicePoints();
        assertNotNull(sps);
        assertFalse(sps.isEmpty(), "Real ILP data contains no service points");

        ServicePoint baseSp = sps.getFirst();
        assertNotNull(baseSp.getLocation(), "Service point location must not be null");

        LocalDate date = LocalDate.of(2025, 1, 6); // Monday

        // Create an impossible requirement: absurdly large capacity
        MedDispatchRec.Requirement req = new MedDispatchRec.Requirement();
        req.setCapacity(1_000_000_000.0); // 1e9, larger than any real drone
        req.setCooling(false);
        req.setHeating(false);
        req.setMaxCost(null);

        MedDispatchRec rec = dispatch(
                9201,
                date,
                LocalTime.of(10, 0),
                baseSp.getLocation(),
                req
        );

        CalcDeliveryPathResponse res = service.calcDeliveryPath(List.of(rec));

        assertNotNull(res, "Response must not be null");
        assertNotNull(res.getDronePaths(), "Drone paths must not be null");

        // For an impossible requirement, we expect an empty plan:
        assertTrue(res.getDronePaths().isEmpty(), "Expected no feasible plan for impossible capacity");
        assertEquals(0, res.getTotalMoves(), "Total moves should be 0 for an empty plan");
        assertEquals(0.0, res.getTotalCost(), 1e-12, "Total cost should be 0 for an empty plan");
    }
}
