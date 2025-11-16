package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Drone;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;
import uk.ac.ed.acp.cw2.data.MedDispatchRec;
import uk.ac.ed.acp.cw2.data.ServicePoint;
import uk.ac.ed.acp.cw2.external.DroneServiceImpl;
import uk.ac.ed.acp.cw2.external.IlpClientComponent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DroneServiceImpl.queryAvailableDrones.
 * Focus:
 * - defensive checks on dispatch list
 * - correct interaction with IlpClientComponent
 * - correct filtering and sorting of drone IDs
 * Helper methods in QueryDroneHelper (availability index, home point index,
 * canHandleAll, isAvailableAt, respectsMaxCost) are tested separately and
 * treated here as a black box.
 */
@ExtendWith(MockitoExtension.class)
class QueryAvailableDronesTests {

    @Mock
    private IlpClientComponent ilpClient;

    @InjectMocks
    private DroneServiceImpl droneService;

    // ---------- Small builders for test data ----------

    private Drone drone(String id, double capacity, boolean cooling, boolean heating,
                        double costPerMove, double costInitial, double costFinal) {
        Drone d = new Drone();
        d.setId(id);
        Drone.DroneCapability cap = new Drone.DroneCapability();
        cap.setCapacity(capacity);
        cap.setCooling(cooling);
        cap.setHeating(heating);
        cap.setCostPerMove(costPerMove);
        cap.setCostInitial(costInitial);
        cap.setCostFinal(costFinal);
        d.setCapability(cap);
        return d;
    }

    private ServicePoint servicePoint(int id, double lng, double lat) {
        ServicePoint sp = new ServicePoint();
        sp.setId(id);
        Coordinate c = new Coordinate();
        c.setLng(lng);
        c.setLat(lat);
        sp.setLocation(c);
        return sp;
    }

    private MedDispatchRec.Requirement requirement(Double capacity,
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

    private MedDispatchRec dispatch(LocalDate date, LocalTime time,
                                    double lng, double lat,
                                    MedDispatchRec.Requirement req) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setDate(date);
        rec.setTime(time);
        rec.setRequirements(req);

        Coordinate target = new Coordinate();
        target.setLng(lng);
        target.setLat(lat);
        rec.setDelivery(target);

        return rec;
    }

    private DroneForServicePoint.Availability window(String day, String from, String until) {
        DroneForServicePoint.Availability w = new DroneForServicePoint.Availability();
        w.setDayOfWeek(day);
        w.setFrom(from);
        w.setUntil(until);
        return w;
    }

    private DroneForServicePoint dfsp(int servicePointId, DroneForServicePoint.Item... items) {
        DroneForServicePoint entry = new DroneForServicePoint();
        entry.setServicePointId(servicePointId);
        List<DroneForServicePoint.Item> list = new ArrayList<>();
        Collections.addAll(list, items);
        entry.setDrones(list);
        return entry;
    }

    private DroneForServicePoint.Item dfspItem(String droneId,
                                               List<DroneForServicePoint.Availability> av) {
        DroneForServicePoint.Item it = new DroneForServicePoint.Item();
        it.setId(droneId);
        it.setAvailability(av);
        return it;
    }

    // ---------- Defensive checks on dispatch list ----------

    @Test
    void nullDispatches_returnsEmptyAndDoesNotCallIlp() {
        List<String> result = droneService.queryAvailableDrones(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(ilpClient, never()).getAllDrones();
        verify(ilpClient, never()).getDronesForServicePoints();
        verify(ilpClient, never()).getServicePoints();
    }

    @Test
    void emptyDispatches_returnsEmptyAndDoesNotCallIlp() {
        List<String> result = droneService.queryAvailableDrones(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(ilpClient, never()).getAllDrones();
        verify(ilpClient, never()).getDronesForServicePoints();
        verify(ilpClient, never()).getServicePoints();
    }

    @Test
    void dispatchWithNullRequirements_returnsEmptyAndDoesNotCallIlp() {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setRequirements(null);

        List<String> result = droneService.queryAvailableDrones(List.of(rec));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(ilpClient, never()).getAllDrones();
        verify(ilpClient, never()).getDronesForServicePoints();
        verify(ilpClient, never()).getServicePoints();
    }

    @Test
    void dispatchWithBothCoolingAndHeating_returnsEmptyAndDoesNotCallIlp() {
        MedDispatchRec.Requirement req = requirement(1.0, true, true, null);
        MedDispatchRec rec = new MedDispatchRec();
        rec.setRequirements(req);

        List<String> result = droneService.queryAvailableDrones(List.of(rec));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(ilpClient, never()).getAllDrones();
        verify(ilpClient, never()).getDronesForServicePoints();
        verify(ilpClient, never()).getServicePoints();
    }

    // ---------- Normal path: one drone can handle, one cannot ----------

    @Test
    void oneDroneCanHandle_otherCannot_returnsOnlyHandlingDroneSorted() {
        // Dispatch: Monday 10:00, capacity 1.0, no maxCost, no cooling/heating
        LocalDate date = LocalDate.of(2025, 1, 6); // Monday
        LocalTime time = LocalTime.of(10, 0);
        MedDispatchRec.Requirement req = requirement(1.0, false, false, null);
        MedDispatchRec dispatch = dispatch(date, time, -3.19, 55.94, req);
        List<MedDispatchRec> dispatches = List.of(dispatch);

        // Drones: A and B (unsorted order: B, A)
        Drone droneA = drone("A", 10.0, true, true,
                1.0, 1.0, 1.0);
        Drone droneB = drone("B", 10.0, true, true,
                1.0, 1.0, 1.0);
        when(ilpClient.getAllDrones()).thenReturn(List.of(droneB, droneA));

        // Service point at same coordinate as delivery (distance 0)
        ServicePoint sp = servicePoint(1, -3.19, 55.94);
        when(ilpClient.getServicePoints()).thenReturn(List.of(sp));

        // Availability:
        // - A: Monday 09:00-17:00 -> available at 10:00
        // - B: no availability -> cannot handle (isAvailableAt false)
        List<DroneForServicePoint.Availability> avA = List.of(
                window("MONDAY", "09:00:00", "17:00:00")
        );
        List<DroneForServicePoint.Availability> avB = List.of(); // empty

        DroneForServicePoint.Item itemA = dfspItem("A", avA);
        DroneForServicePoint.Item itemB = dfspItem("B", avB);

        DroneForServicePoint dfspEntry = dfsp(1, itemA, itemB);
        when(ilpClient.getDronesForServicePoints()).thenReturn(List.of(dfspEntry));

        // Act
        List<String> result = droneService.queryAvailableDrones(dispatches);

        // Assert: only A can handle, and result is sorted
        assertEquals(List.of("A"), result);
        verify(ilpClient, times(1)).getAllDrones();
        verify(ilpClient, times(1)).getDronesForServicePoints();
        verify(ilpClient, times(1)).getServicePoints();
    }

    @Test
    void bothDronesCanHandle_returnsBothIdsSorted() {
        // Same dispatch as before
        LocalDate date = LocalDate.of(2025, 1, 6); // Monday
        LocalTime time = LocalTime.of(10, 0);
        MedDispatchRec.Requirement req = requirement(1.0, false, false, null);
        MedDispatchRec dispatch = dispatch(date, time, -3.19, 55.94, req);
        List<MedDispatchRec> dispatches = List.of(dispatch);

        // Drones: A and B
        Drone droneA = drone("A", 10.0, true, true, 1.0, 1.0, 1.0);
        Drone droneB = drone("B", 10.0, true, true, 1.0, 1.0, 1.0);
        when(ilpClient.getAllDrones()).thenReturn(List.of(droneB, droneA));

        // Service point at same coordinate
        ServicePoint sp = servicePoint(1, -3.19, 55.94);
        when(ilpClient.getServicePoints()).thenReturn(List.of(sp));

        // Both drones have the same good availability
        List<DroneForServicePoint.Availability> av = List.of(
                window("MONDAY", "09:00:00", "17:00:00")
        );
        DroneForServicePoint.Item itemA = dfspItem("A", av);
        DroneForServicePoint.Item itemB = dfspItem("B", av);

        DroneForServicePoint dfspEntry = dfsp(1, itemA, itemB);
        when(ilpClient.getDronesForServicePoints()).thenReturn(List.of(dfspEntry));

        // Act
        List<String> result = droneService.queryAvailableDrones(dispatches);

        // Assert: both can handle, ids sorted
        assertEquals(List.of("A", "B"), result);
    }

    @Test
    void noDroneCanHandle_returnsEmptyList() {
        // Dispatch that requires more capacity than drones have
        LocalDate date = LocalDate.of(2025, 1, 6);
        LocalTime time = LocalTime.of(10, 0);
        MedDispatchRec.Requirement req = requirement(100.0, false, false, null);
        MedDispatchRec dispatch = dispatch(date, time, -3.19, 55.94, req);
        List<MedDispatchRec> dispatches = List.of(dispatch);

        // Drones with small capacity
        Drone droneA = drone("A", 10.0, true, true, 1.0, 1.0, 1.0);
        Drone droneB = drone("B", 5.0, true, true, 1.0, 1.0, 1.0);
        when(ilpClient.getAllDrones()).thenReturn(List.of(droneA, droneB));

        // Valid service point and availability
        ServicePoint sp = servicePoint(1, -3.19, 55.94);
        when(ilpClient.getServicePoints()).thenReturn(List.of(sp));

        List<DroneForServicePoint.Availability> av = List.of(
                window("MONDAY", "09:00:00", "17:00:00")
        );
        DroneForServicePoint.Item itemA = dfspItem("A", av);
        DroneForServicePoint.Item itemB = dfspItem("B", av);
        DroneForServicePoint dfspEntry = dfsp(1, itemA, itemB);
        when(ilpClient.getDronesForServicePoints()).thenReturn(List.of(dfspEntry));

        // Act
        List<String> result = droneService.queryAvailableDrones(dispatches);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void droneWithoutDfspMapping_getsNoHomePoints_cannotHandle() {
        // Dispatch is otherwise valid
        LocalDate date = LocalDate.of(2025, 1, 6);
        LocalTime time = LocalTime.of(10, 0);
        MedDispatchRec.Requirement req = requirement(1.0, false, false, null);
        MedDispatchRec dispatch = dispatch(date, time, -3.19, 55.94, req);
        List<MedDispatchRec> dispatches = List.of(dispatch);

        Drone droneX = drone("X", 10.0, true, true,
                1.0, 1.0, 1.0);
        when(ilpClient.getAllDrones()).thenReturn(List.of(droneX));

        // Service points exist but dfsp list does not mention drone "X"
        ServicePoint sp = servicePoint(1, -3.19, 55.94);
        when(ilpClient.getServicePoints()).thenReturn(List.of(sp));

        // dfspList empty: buildHomePointIndex returns empty mapping for all ids,
        // so homePoints.getOrDefault("X", emptyList) is empty -> canHandleAll returns false
        when(ilpClient.getDronesForServicePoints()).thenReturn(List.of());

        // Act
        List<String> result = droneService.queryAvailableDrones(dispatches);

        // Assert: no available drones
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
