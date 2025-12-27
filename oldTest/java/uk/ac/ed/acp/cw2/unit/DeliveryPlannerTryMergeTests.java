package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.utility.DeliveryPlanner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryPlannerTryMergeTests {

    private Coordinate coord(double lng, double lat) {
        Coordinate c = new Coordinate();
        c.setLng(lng);
        c.setLat(lat);
        return c;
    }

    private ServicePoint servicePoint(int id, double lng, double lat) {
        ServicePoint sp = new ServicePoint();
        sp.setId(id);
        sp.setLocation(coord(lng, lat));
        return sp;
    }

    private Drone drone(String id,
                        double capacity,
                        int maxMoves,
                        double costPerMove,
                        double costInitial,
                        double costFinal,
                        boolean cooling,
                        boolean heating) {
        Drone d = new Drone();
        d.setId(id);
        Drone.DroneCapability cap = new Drone.DroneCapability();
        cap.setCapacity(capacity);
        cap.setMaxMoves(maxMoves);
        cap.setCostPerMove(costPerMove);
        cap.setCostInitial(costInitial);
        cap.setCostFinal(costFinal);
        cap.setCooling(cooling);
        cap.setHeating(heating);
        d.setCapability(cap);
        return d;
    }

    private MedDispatchRec.Requirement requirement(double capacity,
                                                   boolean cooling,
                                                   boolean heating,
                                                   Double maxCost) {
        MedDispatchRec.Requirement req = new MedDispatchRec.Requirement();
        req.setCapacity(capacity);
        req.setCooling(cooling);
        req.setHeating(heating);
        req.setMaxCost(maxCost);
        return req;
    }

    private MedDispatchRec dispatch(int id,
                                    LocalDate date,
                                    LocalTime time,
                                    Coordinate delivery,
                                    MedDispatchRec.Requirement req) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(id);
        rec.setDate(date);
        rec.setTime(time);
        rec.setDelivery(delivery);
        rec.setRequirements(req);
        return rec;
    }

    private DroneForServicePoint.Availability availability(String day, String from, String until) {
        DroneForServicePoint.Availability a = new DroneForServicePoint.Availability();
        a.setDayOfWeek(day);
        a.setFrom(from);
        a.setUntil(until);
        return a;
    }

    private DroneForServicePoint.Item dfspItem(String droneId,
                                               List<DroneForServicePoint.Availability> av) {
        DroneForServicePoint.Item it = new DroneForServicePoint.Item();
        it.setId(droneId);
        it.setAvailability(av);
        return it;
    }

    private DroneForServicePoint dfsp(int servicePointId, DroneForServicePoint.Item... items) {
        DroneForServicePoint d = new DroneForServicePoint();
        d.setServicePointId(servicePointId);
        d.setDrones(Arrays.asList(items));
        return d;
    }

    @Test
    void tryMergeFlight_successfullyMergesSecondDeliveryIntoActiveFlight() {
        ServicePoint sp = servicePoint(1, -3.19, 55.94);
        LocalDate day = LocalDate.of(2025, 1, 6); // Monday
        LocalTime t1 = LocalTime.of(10, 0);
        LocalTime t2 = LocalTime.of(11, 0);

        Drone d1 = drone("D1",
                10.0,
                100,
                1.0,
                1.0,
                1.0,
                false,
                false);

        Map<String, Drone> droneById = Map.of("D1", d1);

        List<DroneForServicePoint.Availability> av = List.of(
                availability("MONDAY", "09:00:00", "17:00:00")
        );
        DroneForServicePoint.Item item = dfspItem("D1", av);
        DroneForServicePoint dfsp = dfsp(sp.getId(), item);
        Map<Integer, DroneForServicePoint> spMapDrone = Map.of(sp.getId(), dfsp);

        Coordinate loc = sp.getLocation();

        MedDispatchRec.Requirement req1 = requirement(2.0, false, false, null);
        MedDispatchRec first = dispatch(1, day, t1, coord(loc.getLng(), loc.getLat()), req1);

        FlightBuilder fb = new FlightBuilder(
                "D1",
                sp,
                10.0,
                100,
                1.0,
                1.0,
                1.0,
                first
        );
        // First segment: simple forward-with-hover at the same location
        List<Coordinate> seg1Path = List.of(
                coord(loc.getLng(), loc.getLat()),
                coord(loc.getLng(), loc.getLat())
        );
        fb.addSegment(
                1,
                seg1Path,
                seg1Path.size() - 1,
                2.0,
                null,
                false,
                false
        );

        List<FlightBuilder> actives = new ArrayList<>();
        actives.add(fb);
        List<FlightBuilder> finished = new ArrayList<>();

        MedDispatchRec.Requirement req2 = requirement(3.0, false, false, null);
        MedDispatchRec second = dispatch(2, day, t2, coord(loc.getLng(), loc.getLat()), req2);

        boolean merged = DeliveryPlanner.tryMergeFlight(
                second,
                actives,
                finished,
                droneById,
                spMapDrone,
                List.of(),
                List.of(),
                day
        );

        assertTrue(merged, "Second delivery should merge into existing active flight");
        assertEquals(1, actives.size(), "Active flights list should still contain one flight");
        assertTrue(finished.isEmpty(), "No flight should be closed in a successful merge");

        FlightBuilder updated = actives.getFirst();
        assertEquals(2, updated.getDeliveryCount(), "Delivery count should be incremented");
        assertEquals(2, updated.getSegments().size(), "Two delivery segments should be present");
        assertEquals(1, updated.getSegments().get(0).getDeliveryId());
        assertEquals(2, updated.getSegments().get(1).getDeliveryId());
        assertEquals(5.0, updated.getCurrentLoad(), 1e-12);
    }

    @Test
    void tryMergeFlight_failsWhenCapacityExceeded() {
        ServicePoint sp = servicePoint(1, -3.19, 55.94);
        LocalDate day = LocalDate.of(2025, 1, 6);
        LocalTime time = LocalTime.of(10, 0);

        Drone d1 = drone("D1",
                5.0,   // low capacity
                100,
                1.0,
                1.0,
                1.0,
                false,
                false);

        Map<String, Drone> droneById = Map.of("D1", d1);

        List<DroneForServicePoint.Availability> av = List.of(
                availability("MONDAY", "09:00:00", "17:00:00")
        );
        DroneForServicePoint.Item item = dfspItem("D1", av);
        DroneForServicePoint dfsp = dfsp(sp.getId(), item);
        Map<Integer, DroneForServicePoint> spMapDrone = Map.of(sp.getId(), dfsp);

        Coordinate loc = sp.getLocation();

        MedDispatchRec.Requirement req1 = requirement(3.0, false, false, null);
        MedDispatchRec first = dispatch(1, day, time, coord(loc.getLng(), loc.getLat()), req1);

        FlightBuilder fb = new FlightBuilder(
                "D1",
                sp,
                5.0,
                100,
                1.0,
                1.0,
                1.0,
                first
        );
        List<Coordinate> seg1Path = List.of(
                coord(loc.getLng(), loc.getLat()),
                coord(loc.getLng(), loc.getLat())
        );
        fb.addSegment(
                1,
                seg1Path,
                seg1Path.size() - 1,
                3.0,
                null,
                false,
                false
        );

        List<FlightBuilder> actives = new ArrayList<>();
        actives.add(fb);
        List<FlightBuilder> finished = new ArrayList<>();

        MedDispatchRec.Requirement req2 = requirement(4.0, false, false, null);
        MedDispatchRec second = dispatch(2, day, time, coord(loc.getLng(), loc.getLat()), req2);

        boolean merged = DeliveryPlanner.tryMergeFlight(
                second,
                actives,
                finished,
                droneById,
                spMapDrone,
                List.of(),
                List.of(),
                day
        );

        assertFalse(merged, "Merge must fail when capacity would be exceeded");
        assertEquals(1, actives.size(), "Active flight should remain");
        assertTrue(finished.isEmpty(), "No flight should be closed on capacity failure");
        FlightBuilder unchanged = actives.getFirst();
        assertEquals(1, unchanged.getDeliveryCount());
        assertEquals(3.0, unchanged.getCurrentLoad(), 1e-12);
    }

    @Test
    void tryMergeFlight_closesFlightWhenDateDoesNotMatch() {
        ServicePoint sp = servicePoint(1, -3.19, 55.94);
        LocalDate flightDay = LocalDate.of(2025, 1, 6);   // Monday
        LocalDate queryDay  = LocalDate.of(2025, 1, 7);   // Tuesday

        Drone d1 = drone("D1",
                10.0,
                100,
                1.0,
                1.0,
                1.0,
                false,
                false);

        Map<String, Drone> droneById = Map.of("D1", d1);

        List<DroneForServicePoint.Availability> av = List.of(
                availability("MONDAY", "09:00:00", "17:00:00"),
                availability("TUESDAY", "09:00:00", "17:00:00")
        );
        DroneForServicePoint.Item item = dfspItem("D1", av);
        DroneForServicePoint dfsp = dfsp(sp.getId(), item);
        Map<Integer, DroneForServicePoint> spMapDrone = Map.of(sp.getId(), dfsp);

        Coordinate loc = sp.getLocation();

        MedDispatchRec.Requirement req1 = requirement(2.0, false, false, null);
        MedDispatchRec first = dispatch(1, flightDay, LocalTime.of(10, 0),
                coord(loc.getLng(), loc.getLat()), req1);

        FlightBuilder fb = new FlightBuilder(
                "D1",
                sp,
                10.0,
                100,
                1.0,
                1.0,
                1.0,
                first
        );
        List<Coordinate> seg1Path = List.of(
                coord(loc.getLng(), loc.getLat()),
                coord(loc.getLng(), loc.getLat())
        );
        fb.addSegment(
                1,
                seg1Path,
                seg1Path.size() - 1,
                2.0,
                null,
                false,
                false
        );

        List<FlightBuilder> actives = new ArrayList<>();
        actives.add(fb);
        List<FlightBuilder> finished = new ArrayList<>();

        MedDispatchRec.Requirement req2 = requirement(2.0, false, false, null);
        MedDispatchRec second = dispatch(2, queryDay, LocalTime.of(11, 0),
                coord(loc.getLng(), loc.getLat()), req2);

        boolean merged = DeliveryPlanner.tryMergeFlight(
                second,
                actives,
                finished,
                droneById,
                spMapDrone,
                List.of(),
                List.of(),
                queryDay
        );

        assertFalse(merged, "Merge cannot happen when flight date does not match query day");
        assertTrue(actives.isEmpty(), "Active flight list should be cleared for mismatched date");
        assertEquals(1, finished.size(), "Flight should be moved to finished");
    }
}
