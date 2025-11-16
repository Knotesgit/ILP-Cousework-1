package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.external.DroneServiceImpl;
import uk.ac.ed.acp.cw2.external.IlpClientComponent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalcDeliveryPathTests {

    @Mock
    private IlpClientComponent ilpClient;

    @InjectMocks
    private DroneServiceImpl service;

    // ---------- Small helpers ----------

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
        MedDispatchRec.Requirement r = new MedDispatchRec.Requirement();
        r.setCapacity(capacity);
        r.setCooling(cooling);
        r.setHeating(heating);
        r.setMaxCost(maxCost);
        return r;
    }

    private MedDispatchRec dispatch(Integer id,
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

    private DroneForServicePoint dfsp(int servicePointId,
                                      List<DroneForServicePoint.Item> items) {
        DroneForServicePoint d = new DroneForServicePoint();
        d.setServicePointId(servicePointId);
        d.setDrones(items);
        return d;
    }

    private <T> List<T> mutable(T... arr) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, arr);
        return list;
    }

    // ---------- Test 1: invalid inputs ----------

    @Test
    void nullDispatchList_returnsEmptyResponse_andDoesNotCallIlpClient() {
        CalcDeliveryPathResponse res = service.calcDeliveryPath(null);

        assertNotNull(res);
        assertEquals(0.0, res.getTotalCost(), 1e-12);
        assertEquals(0, res.getTotalMoves());
        assertNotNull(res.getDronePaths());
        assertTrue(res.getDronePaths().isEmpty());

        verify(ilpClient, never()).getAllDrones();
        verify(ilpClient, never()).getServicePoints();
        verify(ilpClient, never()).getRestrictedAreas();
        verify(ilpClient, never()).getDronesForServicePoints();
    }

    @Test
    void invalidDispatchList_returnsEmptyResponse_andDoesNotCallIlpClient() {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(null);
        rec.setRequirements(requirement(1.0, false, false, null));
        rec.setDelivery(coord(-3.19, 55.94));
        rec.setDate(LocalDate.of(2025, 1, 6));
        rec.setTime(LocalTime.of(10, 0));

        CalcDeliveryPathResponse res = service.calcDeliveryPath(List.of(rec));

        assertNotNull(res);
        assertEquals(0.0, res.getTotalCost(), 1e-12);
        assertEquals(0, res.getTotalMoves());
        assertNotNull(res.getDronePaths());
        assertTrue(res.getDronePaths().isEmpty());

        verify(ilpClient, never()).getAllDrones();
        verify(ilpClient, never()).getServicePoints();
        verify(ilpClient, never()).getRestrictedAreas();
        verify(ilpClient, never()).getDronesForServicePoints();
    }

    // ---------- Test 2: simplest successful case ----------

    @Test
    void singleDispatch_singleDrone_singleServicePoint_buildsOneFlightAndOneDelivery() {

        LocalDate day = LocalDate.of(2025, 1, 6);
        LocalTime time = LocalTime.of(10, 0);

        ServicePoint sp = servicePoint(1, -3.1900, 55.9440);
        Coordinate delivery = coord(sp.getLocation().getLng(), sp.getLocation().getLat());

        MedDispatchRec rec = dispatch(
                1001, day, time, delivery,
                requirement(2.0, false, false, null)
        );

        Drone d1 = drone("D1", 10.0, 100, 2.0, 1.0, 3.0, false, false);

        List<DroneForServicePoint.Availability> av = mutable(
                availability("MONDAY", "09:00:00", "17:00:00")
        );
        DroneForServicePoint.Item item = dfspItem("D1", av);
        DroneForServicePoint dfsp = dfsp(sp.getId(), mutable(item));

        when(ilpClient.getAllDrones()).thenReturn(mutable(d1));
        when(ilpClient.getServicePoints()).thenReturn(mutable(sp));
        when(ilpClient.getRestrictedAreas()).thenReturn(new ArrayList<>());
        when(ilpClient.getDronesForServicePoints()).thenReturn(mutable(dfsp));

        List<MedDispatchRec> recs = new ArrayList<>();
        recs.add(rec);
        CalcDeliveryPathResponse res = service.calcDeliveryPath(recs);

        assertNotNull(res);
        assertEquals(6.0, res.getTotalCost(), 1e-9);
        assertEquals(1, res.getTotalMoves());

        assertNotNull(res.getDronePaths());
        assertEquals(1, res.getDronePaths().size());

        CalcDeliveryPathResponse.DronePath dp = res.getDronePaths().getFirst();
        assertEquals("D1", dp.getDroneId());
        assertNotNull(dp.getDeliveries());
        assertEquals(2, dp.getDeliveries().size());
    }

    // ---------- Test 3: no feasible flight ----------

    @Test
    void noAvailableDroneForServicePoints_returnsEmptyResponse() {

        LocalDate day = LocalDate.of(2025, 1, 6);
        LocalTime time = LocalTime.of(10, 0);

        ServicePoint sp = servicePoint(1, -3.1900, 55.9440);
        Coordinate delivery = coord(sp.getLocation().getLng(), sp.getLocation().getLat());

        MedDispatchRec rec = dispatch(
                2001, day, time, delivery,
                requirement(2.0, false, false, null)
        );

        Drone d1 = drone("D1", 10.0, 100, 1.0, 1.0, 1.0, false, false);

        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(sp.getId());
        dfsp.setDrones(null);

        when(ilpClient.getAllDrones()).thenReturn(mutable(d1));
        when(ilpClient.getServicePoints()).thenReturn(mutable(sp));
        when(ilpClient.getRestrictedAreas()).thenReturn(new ArrayList<>());
        when(ilpClient.getDronesForServicePoints()).thenReturn(mutable(dfsp));

        CalcDeliveryPathResponse res = service.calcDeliveryPath(List.of(rec));

        assertNotNull(res);
        assertEquals(0.0, res.getTotalCost(), 1e-12);
        assertEquals(0, res.getTotalMoves());
        assertTrue(res.getDronePaths().isEmpty());
    }

    // ---------- Test 4: ordering ----------

    @Test
    void twoDispatchesSameDaySameTime_areHandledInIdOrder() {

        LocalDate day = LocalDate.of(2025, 1, 6);
        LocalTime time = LocalTime.of(10, 0);

        ServicePoint sp = servicePoint(1, -3.1900, 55.9440);
        Coordinate delivery = coord(sp.getLocation().getLng(), sp.getLocation().getLat());

        MedDispatchRec recHighId = dispatch(
                200, day, time, delivery,
                requirement(1.0, false, false, null)
        );
        MedDispatchRec recLowId = dispatch(
                100, day, time, delivery,
                requirement(1.0, false, false, null)
        );

        Drone d1 = drone("D1", 10.0, 100, 1.0, 1.0, 1.0, false, false);

        List<DroneForServicePoint.Availability> av = mutable(
                availability("MONDAY", "09:00:00", "17:00:00")
        );
        DroneForServicePoint.Item item = dfspItem("D1", av);
        DroneForServicePoint dfsp = dfsp(sp.getId(), mutable(item));

        when(ilpClient.getAllDrones()).thenReturn(mutable(d1));
        when(ilpClient.getServicePoints()).thenReturn(mutable(sp));
        when(ilpClient.getRestrictedAreas()).thenReturn(new ArrayList<>());
        when(ilpClient.getDronesForServicePoints()).thenReturn(mutable(dfsp));

        List<MedDispatchRec> input = mutable(recHighId, recLowId);

        CalcDeliveryPathResponse res = service.calcDeliveryPath(input);

        assertNotNull(res);
        assertNotNull(res.getDronePaths());
        assertEquals(1, res.getDronePaths().size());

        CalcDeliveryPathResponse.DronePath dp = res.getDronePaths().getFirst();
        List<CalcDeliveryPathResponse.DeliverySegment> segs = dp.getDeliveries();

        CalcDeliveryPathResponse.DeliverySegment seg0 = segs.get(0);
        CalcDeliveryPathResponse.DeliverySegment seg1 = segs.get(1);

        assertEquals(100, seg0.getDeliveryId());
        assertEquals(200, seg1.getDeliveryId());
    }
}
