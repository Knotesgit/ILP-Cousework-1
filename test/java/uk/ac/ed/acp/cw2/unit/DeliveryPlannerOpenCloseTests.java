package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.utility.DeliveryPlanner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryPlannerOpenCloseTests {

    private Coordinate coord(double lng, double lat) {
        Coordinate c = new Coordinate();
        c.setLng(lng);
        c.setLat(lat);
        return c;
    }

    private ServicePoint servicePoint() {
        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        sp.setLocation(coord(-3.19, 55.94));
        return sp;
    }

    private Drone drone() {
        Drone d = new Drone();
        d.setId("D1");
        Drone.DroneCapability cap = new Drone.DroneCapability();
        cap.setCapacity(10.0);
        cap.setMaxMoves(100);
        cap.setCostPerMove(1.0);
        cap.setCostInitial(1.0);
        cap.setCostFinal(1.0);
        cap.setCooling(false);
        cap.setHeating(false);
        d.setCapability(cap);
        return d;
    }

    private MedDispatchRec.Requirement requirement(double capacity) {
        MedDispatchRec.Requirement req = new MedDispatchRec.Requirement();
        req.setCapacity(capacity);
        req.setCooling(false);
        req.setHeating(false);
        req.setMaxCost(null);
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

    private DroneForServicePoint.Availability availability() {
        DroneForServicePoint.Availability a = new DroneForServicePoint.Availability();
        a.setDayOfWeek("MONDAY");
        a.setFrom("09:00:00");
        a.setUntil("17:00:00");
        return a;
    }

    private DroneForServicePoint dfsp(int servicePointId, DroneForServicePoint.Item... items) {
        DroneForServicePoint d = new DroneForServicePoint();
        d.setServicePointId(servicePointId);
        List<DroneForServicePoint.Item> list = new ArrayList<>();
        Collections.addAll(list, items);
        d.setDrones(list);
        return d;
    }

    private DroneForServicePoint.Item dfspItem(List<DroneForServicePoint.Availability> av) {
        DroneForServicePoint.Item it = new DroneForServicePoint.Item();
        it.setId("D1");
        it.setAvailability(av);
        return it;
    }

    @Test
    void openNewFlight_createsFlightWithInitialSegment() {
        // One service point and one drone, delivery at the same location.
        ServicePoint sp = servicePoint();
        List<ServicePoint> spCandidates = new ArrayList<>();
        spCandidates.add(sp);

        Drone d1 = drone(
        );

        Map<String, Drone> droneById = Map.of("D1", d1);

        // Drone D1 available on Monday 09:00–17:00.
        List<DroneForServicePoint.Availability> av = List.of(
                availability()
        );
        DroneForServicePoint.Item item = dfspItem(av);
        DroneForServicePoint dfsp = dfsp(sp.getId(), item);
        Map<Integer, DroneForServicePoint> spMapDrone = Map.of(sp.getId(), dfsp);

        LocalDate day = LocalDate.of(2025, 1, 6); // Monday
        LocalTime time = LocalTime.of(10, 0);

        MedDispatchRec.Requirement req = requirement(
                2.0
        );
        // Delivery at exactly the same coordinate as the service point
        Coordinate target = coord(sp.getLocation().getLng(), sp.getLocation().getLat());
        MedDispatchRec rec = dispatch(100, day, time, target, req);

        FlightBuilder fb = DeliveryPlanner.openNewFlight(
                spCandidates,
                droneById,
                spMapDrone,
                rec,
                List.of(),       // no restricted polygons
                List.of(),       // no bounding boxes
                day
        );

        assertNotNull(fb, "openNewFlight should return a flight in this simple case");

        assertEquals("D1", fb.getDroneId());
        assertEquals(sp.getId(), fb.getServicePoint().getId());
        assertEquals(10.0, fb.getCapacity(), 1e-12);
        assertEquals(100, fb.getMaxMoves());

        // There should be exactly one segment for this first delivery.
        assertNotNull(fb.getSegments());
        assertEquals(1, fb.getSegments().size());

        var seg = fb.getSegments().getFirst();
        assertEquals(100, seg.getDeliveryId());
        assertNotNull(seg.getFlightPath());
        assertTrue(seg.getFlightPath().size() >= 1);

        // Steps used should match the steps argument passed inside openNewFlight.
        assertTrue(fb.getStepsUsed() >= 1);
        assertEquals(1, fb.getDeliveryCount());
    }

    @Test
    void openNewFlight_returnsNullWhenNoAvailableDrone() {
        // Service point exists but no DroneForServicePoint mapping and no availability.
        ServicePoint sp = servicePoint();
        List<ServicePoint> spCandidates = new ArrayList<>();
        spCandidates.add(sp);

        Drone d1 = drone(
        );

        Map<String, Drone> droneById = Map.of("D1", d1);

        // spMapDrone has an entry with null/empty drones, so feasibleDroneIdsAtSP should see no drones.
        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(sp.getId());
        dfsp.setDrones(null);
        Map<Integer, DroneForServicePoint> spMapDrone = Map.of(sp.getId(), dfsp);

        LocalDate day = LocalDate.of(2025, 1, 6);
        LocalTime time = LocalTime.of(10, 0);

        MedDispatchRec.Requirement req = requirement(2.0);
        Coordinate target = coord(sp.getLocation().getLng(), sp.getLocation().getLat());
        MedDispatchRec rec = dispatch(100, day, time, target, req);

        FlightBuilder fb = DeliveryPlanner.openNewFlight(
                spCandidates,
                droneById,
                spMapDrone,
                rec,
                List.of(),
                List.of(),
                day
        );

        assertNull(fb, "openNewFlight should return null when no drones are available at the service point");
    }

    @Test
    void closeFlight_singleDelivery_addsReturnSegmentAndKeepsSteps() {
        ServicePoint sp = servicePoint();
        MedDispatchRec first = dispatch(
                1,
                LocalDate.of(2025, 1, 6),
                LocalTime.of(10, 0),
                coord(sp.getLocation().getLng(), sp.getLocation().getLat()),
                requirement(1.0)
        );

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

        // Build a simple forward-with-hover path: S -> S
        Coordinate s = sp.getLocation();
        List<Coordinate> forwardWithHover = List.of(
                coord(s.getLng(), s.getLat()),
                coord(s.getLng(), s.getLat())
        );
        int steps = forwardWithHover.size() - 1; // 1

        fb.addSegment(
                1,
                forwardWithHover,
                steps,
                1.0,
                null,
                false,
                false
        );

        assertEquals(1, fb.getDeliveryCount());
        assertEquals(1, fb.getStepsUsed());

        List<FlightBuilder> finished = new ArrayList<>();

        DeliveryPlanner.closeFlight(
                fb,
                finished,
                List.of(),
                List.of()
        );

        assertEquals(1, finished.size());
        FlightBuilder closed = finished.getFirst();

        // For a single-delivery flight, closeFlight should append a return segment.
        assertNotNull(closed.getSegments());
        assertEquals(2, closed.getSegments().size());

        var segDelivery = closed.getSegments().get(0);
        var segReturn = closed.getSegments().get(1);

        assertEquals(1, segDelivery.getDeliveryId());
        assertNull(segReturn.getDeliveryId());

        assertEquals(1, closed.getStepsUsed(),
                "Steps used should not increase when the return path has zero steps");
        assertEquals(sp.getLocation().getLng(), closed.getEnd().getLng(), 1e-12);
        assertEquals(sp.getLocation().getLat(), closed.getEnd().getLat(), 1e-12);
    }
}

