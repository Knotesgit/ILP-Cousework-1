package uk.ac.ed.acp.cw2.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Drone;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;
import uk.ac.ed.acp.cw2.data.MedDispatchRec;
import uk.ac.ed.acp.cw2.data.RestrictedArea;
import uk.ac.ed.acp.cw2.data.ServicePoint;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.external.DroneServiceImpl;
import uk.ac.ed.acp.cw2.external.IlpClientComponent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DroneServiceImplDroneSelectionTest {

    private static Drone drone(String id, boolean cooling, boolean heating, double capacity) {
        Drone d = new Drone();
        d.setId(id);

        Drone.DroneCapability cap = new Drone.DroneCapability();
        cap.setCooling(cooling);
        cap.setHeating(heating);
        cap.setCapacity(capacity);

        // Make all drones trivially able to fly (avoid maxMoves filtering)
        cap.setMaxMoves(10_000);

        // Make costs irrelevant for these tests (we want constraint filtering, not cost ranking)
        cap.setCostInitial(0.0);
        cap.setCostFinal(0.0);
        cap.setCostPerMove(0.0);

        d.setCapability(cap);
        return d;
    }

    private static ServicePoint sp(int id, double lng, double lat) {
        ServicePoint sp = new ServicePoint();
        sp.setId(id);
        sp.setName("SP-" + id);
        sp.setLocation(new Coordinate(lng, lat));
        return sp;
    }

    private static DroneForServicePoint dfsp(int servicePointId, String... droneIds) {
        DroneForServicePoint entry = new DroneForServicePoint();
        entry.setServicePointId(servicePointId);

        List<DroneForServicePoint.Item> items = Arrays.stream(droneIds).map(id -> {
            DroneForServicePoint.Item it = new DroneForServicePoint.Item();
            it.setId(id);
            // Make drones available on any day so availability does not interfere with capability/capacity filtering.
            it.setAvailability(new java.util.ArrayList<>(List.of(
                    new DroneForServicePoint.Availability("MONDAY",    "00:00:00", "23:59:59"),
                    new DroneForServicePoint.Availability("TUESDAY",   "00:00:00", "23:59:59"),
                    new DroneForServicePoint.Availability("WEDNESDAY", "00:00:00", "23:59:59"),
                    new DroneForServicePoint.Availability("THURSDAY",  "00:00:00", "23:59:59"),
                    new DroneForServicePoint.Availability("FRIDAY",    "00:00:00", "23:59:59"),
                    new DroneForServicePoint.Availability("SATURDAY",  "00:00:00", "23:59:59"),
                    new DroneForServicePoint.Availability("SUNDAY",    "00:00:00", "23:59:59")
            )));
            return it;
        }).toList();

        entry.setDrones(items);
        return entry;
    }

    private static MedDispatchRec dispatch(int deliveryId, double capacity, boolean cooling, boolean heating,
                                           double lng, double lat) {
        MedDispatchRec r = new MedDispatchRec();
        r.setId(deliveryId);
        r.setDelivery(new Coordinate(lng, lat));

        MedDispatchRec.Requirement req = new MedDispatchRec.Requirement();
        req.setCapacity(capacity);
        req.setCooling(cooling);
        req.setHeating(heating);
        r.setRequirements(req);
        // Provide a date so calcDeliveryPath enters the scheduling loop (date-only dispatch).
        // Time is intentionally null because these tests are not about time-window scheduling.
        r.setDate(LocalDate.of(2025, 12, 22));
        r.setTime(null);

        return r;
    }

    @Test
    @DisplayName("FR-S3-1: cooling-required dispatch excludes non-cooling drones")
    void coolingRequirement_filtersDrones() {
        IlpClientComponent ilp = mock(IlpClientComponent.class);

        Drone d1 = drone("D_NO_COOL", false, false, 10.0);
        Drone d2 = drone("D_COOL",    true,  false, 10.0);
        // Place delivery at the service point to avoid pathfinding being a confounding factor.
        ServicePoint sp = sp(1, 0.0, 0.0);
        MedDispatchRec rec = dispatch(1, 1.0, true, false, 0.0, 0.0);

        when(ilp.getAllDrones()).thenReturn(new ArrayList<>(List.of(d1, d2)));
        when(ilp.getServicePoints()).thenReturn(new ArrayList<>(List.of(sp)));
        when(ilp.getRestrictedAreas()).thenReturn(new ArrayList<RestrictedArea>());
        when(ilp.getDronesForServicePoints()).thenReturn(new ArrayList<>(List.of(dfsp(1, "D_NO_COOL", "D_COOL"))));

        DroneServiceImpl sut = new DroneServiceImpl(ilp);

        CalcDeliveryPathResponse resp = sut.calcDeliveryPath(new ArrayList<>(List.of(rec)));

        assertNotNull(resp);
        assertNotNull(resp.getDronePaths());
        assertEquals(1, resp.getDronePaths().size(), "Exactly one drone should be assigned for a single dispatch");
        assertEquals("D_COOL", resp.getDronePaths().get(0).getDroneId(), "Only cooling-capable drone may be selected");
    }

    @Test
    @DisplayName("FR-S3-2: capacity requirement excludes drones with insufficient capacity")
    void capacityRequirement_filtersDrones() {
        IlpClientComponent ilp = mock(IlpClientComponent.class);

        Drone small = drone("D_SMALL", false, false, 1.0);
        Drone big   = drone("D_BIG",   false, false, 5.0);

        ServicePoint sp = sp(1, 0.0, 0.0);
        MedDispatchRec rec = dispatch(2, 2.0, false, false, 0.0, 0.0);

        when(ilp.getAllDrones()).thenReturn(new ArrayList<>(List.of(small, big)));
        when(ilp.getServicePoints()).thenReturn(new ArrayList<>(List.of(sp)));
        when(ilp.getRestrictedAreas()).thenReturn(new ArrayList<RestrictedArea>());
        when(ilp.getDronesForServicePoints()).thenReturn(new ArrayList<>(List.of(dfsp(1, "D_SMALL", "D_BIG"))));

        DroneServiceImpl sut = new DroneServiceImpl(ilp);

        CalcDeliveryPathResponse resp = sut.calcDeliveryPath(new ArrayList<>(List.of(rec)));

        assertNotNull(resp);
        assertNotNull(resp.getDronePaths());
        assertEquals(1, resp.getDronePaths().size());
        assertEquals("D_BIG", resp.getDronePaths().get(0).getDroneId(), "Only sufficient-capacity drone may be selected");
    }

    @Test
    @DisplayName("FR-S3-3: heating-required dispatch excludes non-heating drones")
    void heatingRequirement_filtersDrones() {
        IlpClientComponent ilp = mock(IlpClientComponent.class);

        Drone noHeat = drone("D_NO_HEAT", false, false, 10.0);
        Drone heat   = drone("D_HEAT",    false, true,  10.0);

        ServicePoint sp = sp(1, 0.0, 0.0);
        MedDispatchRec rec = dispatch(3, 1.0, false, true, 0.0, 0.0);

        when(ilp.getAllDrones()).thenReturn(new ArrayList<>(List.of(noHeat, heat)));
        when(ilp.getServicePoints()).thenReturn(new ArrayList<>(List.of(sp)));
        when(ilp.getRestrictedAreas()).thenReturn(new ArrayList<RestrictedArea>());
        when(ilp.getDronesForServicePoints()).thenReturn(new ArrayList<>(List.of(dfsp(1, "D_NO_HEAT", "D_HEAT"))));

        DroneServiceImpl sut = new DroneServiceImpl(ilp);

        CalcDeliveryPathResponse resp = sut.calcDeliveryPath(new ArrayList<>(List.of(rec)));

        assertNotNull(resp);
        assertNotNull(resp.getDronePaths());
        assertEquals(1, resp.getDronePaths().size());
        assertEquals("D_HEAT", resp.getDronePaths().get(0).getDroneId(), "Only heating-capable drone may be selected");
    }

    @Test
    @DisplayName("FR-S3-4: heating+capacity combined constraints exclude drones failing either")
    void heatingAndCapacity_combinedFilters() {
        IlpClientComponent ilp = mock(IlpClientComponent.class);

        // Need: heating=true and capacity>=2.0
        Drone heatSmall = drone("D_HEAT_SMALL", false, true,  1.0);  // fails capacity
        Drone bigNoHeat = drone("D_BIG_NO_HEAT", false, false, 10.0); // fails heating
        Drone heatBig   = drone("D_HEAT_BIG",   false, true,  5.0);  // passes both

        ServicePoint sp = sp(1, 0.0, 0.0);
        MedDispatchRec rec = dispatch(4, 2.0, false, true, 0.0, 0.0);

        when(ilp.getAllDrones()).thenReturn(new ArrayList<>(List.of(heatSmall, bigNoHeat, heatBig)));
        when(ilp.getServicePoints()).thenReturn(new ArrayList<>(List.of(sp)));
        when(ilp.getRestrictedAreas()).thenReturn(new ArrayList<RestrictedArea>());
        when(ilp.getDronesForServicePoints()).thenReturn(new ArrayList<>(List.of(
                dfsp(1, "D_HEAT_SMALL", "D_BIG_NO_HEAT", "D_HEAT_BIG")
        )));

        DroneServiceImpl sut = new DroneServiceImpl(ilp);
        CalcDeliveryPathResponse resp = sut.calcDeliveryPath(new ArrayList<>(List.of(rec)));

        assertNotNull(resp);
        assertNotNull(resp.getDronePaths());
        assertEquals(1, resp.getDronePaths().size());
        assertEquals("D_HEAT_BIG", resp.getDronePaths().get(0).getDroneId(),
                "Selected drone must satisfy BOTH heating and capacity constraints");
    }

    @Test
    @DisplayName("FR-S3-5: cooling+capacity combined constraints exclude drones failing either")
    void coolingAndCapacity_combinedFilters() {
        IlpClientComponent ilp = mock(IlpClientComponent.class);

        // Need: cooling=true and capacity>=2.0
        Drone coolSmall = drone("D_COOL_SMALL",  true,  false, 1.0);   // fails capacity
        Drone bigNoCool = drone("D_BIG_NO_COOL", false, false, 10.0);  // fails cooling
        Drone coolBig   = drone("D_COOL_BIG",    true,  false, 5.0);   // passes both

        ServicePoint sp = sp(1, 0.0, 0.0);
        MedDispatchRec rec = dispatch(5, 2.0, true, false, 0.0, 0.0);

        when(ilp.getAllDrones()).thenReturn(new ArrayList<>(List.of(coolSmall, bigNoCool, coolBig)));
        when(ilp.getServicePoints()).thenReturn(new ArrayList<>(List.of(sp)));
        when(ilp.getRestrictedAreas()).thenReturn(new ArrayList<RestrictedArea>());
        when(ilp.getDronesForServicePoints()).thenReturn(new ArrayList<>(List.of(
                dfsp(1, "D_COOL_SMALL", "D_BIG_NO_COOL", "D_COOL_BIG")
        )));

        DroneServiceImpl sut = new DroneServiceImpl(ilp);
        CalcDeliveryPathResponse resp = sut.calcDeliveryPath(new ArrayList<>(List.of(rec)));

        assertNotNull(resp);
        assertNotNull(resp.getDronePaths());
        assertEquals(1, resp.getDronePaths().size());
        assertEquals("D_COOL_BIG", resp.getDronePaths().get(0).getDroneId(),
                "Selected drone must satisfy BOTH cooling and capacity constraints");
    }

    @Test
    @DisplayName("FR-S3-6: multiple eligible drones -> selected drone must be one of eligible set (no tie-break assumed)")
    void multipleEligible_acceptAnyEligible() {
        IlpClientComponent ilp = mock(IlpClientComponent.class);

        // Need: capacity>=1, no cooling/heating -> both eligible
        Drone a = drone("D_A", false, false, 10.0);
        Drone b = drone("D_B", false, false, 10.0);

        ServicePoint sp = sp(1, 0.0, 0.0);
        MedDispatchRec rec = dispatch(6, 1.0, false, false, 0.0, 0.0);

        when(ilp.getAllDrones()).thenReturn(new ArrayList<>(List.of(a, b)));
        when(ilp.getServicePoints()).thenReturn(new ArrayList<>(List.of(sp)));
        when(ilp.getRestrictedAreas()).thenReturn(new ArrayList<RestrictedArea>());
        when(ilp.getDronesForServicePoints()).thenReturn(new ArrayList<>(List.of(dfsp(1, "D_A", "D_B"))));

        DroneServiceImpl sut = new DroneServiceImpl(ilp);
        CalcDeliveryPathResponse resp = sut.calcDeliveryPath(new ArrayList<>(List.of(rec)));

        assertNotNull(resp);
        assertNotNull(resp.getDronePaths());
        assertEquals(1, resp.getDronePaths().size());

        String chosen = resp.getDronePaths().get(0).getDroneId();
        assertTrue(chosen.equals("D_A") || chosen.equals("D_B"),
                "When multiple drones are eligible, implementation may choose any eligible drone unless spec defines tie-break");
    }
}
