package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.utility.QueryDroneHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryDroneHelperCanHandleAllTests {

    private Drone drone(double capacity, boolean cooling) {
        Drone d = new Drone();
        d.setId("D1");
        Drone.DroneCapability cap = new Drone.DroneCapability();
        cap.setCapacity(capacity);
        cap.setCooling(cooling);
        cap.setHeating(true);
        cap.setCostPerMove(1.0);
        cap.setCostInitial(1.0);
        cap.setCostFinal(1.0);
        d.setCapability(cap);
        return d;
    }

    private ServicePoint servicePoint() {
        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        Coordinate c = new Coordinate();
        c.setLng(-3.19);
        c.setLat(55.94);
        sp.setLocation(c);
        return sp;
    }

    private MedDispatchRec dispatch(LocalDate date, LocalTime time,
                                    Double capacityReq,
                                    boolean cooling,
                                    Double maxCost) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setDate(date);
        rec.setTime(time);

        Coordinate target = new Coordinate();
        target.setLng(-3.19);
        target.setLat(55.94);
        rec.setDelivery(target);

        MedDispatchRec.Requirement req = new MedDispatchRec.Requirement();
        req.setCapacity(capacityReq);
        req.setCooling(cooling);
        req.setHeating(false);
        req.setMaxCost(maxCost);
        rec.setRequirements(req);

        return rec;
    }

    private DroneForServicePoint.Availability window() {
        DroneForServicePoint.Availability w = new DroneForServicePoint.Availability();
        w.setDayOfWeek("MONDAY");
        w.setFrom("09:00:00");
        w.setUntil("17:00:00");
        return w;
    }

    // ---------- Basic null / precondition checks ----------

    @Test
    void nullDrone_returnsFalse() {
        boolean result = QueryDroneHelper.canHandleAll(
                null,
                List.of(),
                List.of(),
                List.of(),
                0
        );
        assertFalse(result);
    }

    @Test
    void noHomePoints_returnsFalse() {
        Drone d = drone(10.0, true);

        boolean result = QueryDroneHelper.canHandleAll(
                d,
                List.of(),
                List.of(),    // empty homePoints
                List.of(),    // dispatch list does not matter here
                0
        );
        assertFalse(result);
    }

    // ---------- Happy path without any maxCost ----------

    @Test
    void allRequirementsOk_noMaxCost_returnsTrue() {
        Drone d = drone(10.0, true);

        ServicePoint sp = servicePoint();
        List<ServicePoint> homePoints = List.of(sp);

        // delivery at the same coordinate, zero distance, capacity requirement 3.0 each
        MedDispatchRec r1 = dispatch(
                LocalDate.of(2025, 1, 6), // Monday
                LocalTime.of(10, 0),
                3.0,
                false,
                null
        );
        MedDispatchRec r2 = dispatch(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(11, 0),
                3.0,
                false,
                null
        );

        // availability covers Monday 09:00–17:00
        List<DroneForServicePoint.Availability> windows = List.of(
                window()
        );

        boolean result = QueryDroneHelper.canHandleAll(
                d,
                windows,
                homePoints,
                List.of(r1, r2),
                2
        );
        assertTrue(result);
    }

    // ---------- Capacity and capability failures ----------

    @Test
    void requiredCapacityExceedsDroneCapacity_returnsFalse() {
        Drone d = drone(5.0, true);

        ServicePoint sp = servicePoint();
        List<ServicePoint> homePoints = List.of(sp);

        MedDispatchRec r1 = dispatch(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(10, 0),
                6.0,          // requirement > capacity
                false,
                null
        );

        List<DroneForServicePoint.Availability> windows = List.of(
                window()
        );

        boolean result = QueryDroneHelper.canHandleAll(
                d,
                windows,
                homePoints,
                List.of(r1),
                1
        );
        assertFalse(result);
    }

    @Test
    void coolingRequiredButDroneCannotCool_returnsFalse() {
        Drone d = drone(10.0, false);

        ServicePoint sp = servicePoint();
        List<ServicePoint> homePoints = List.of(sp);

        MedDispatchRec r1 = dispatch(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(10, 0),
                2.0,
                true,  // needs cooling
                null
        );

        List<DroneForServicePoint.Availability> windows = List.of(
                window()
        );

        boolean result = QueryDroneHelper.canHandleAll(
                d,
                windows,
                homePoints,
                List.of(r1),
                1
        );
        assertFalse(result);
    }

    @Test
    void notAvailableAtTime_returnsFalse() {
        Drone d = drone(10.0, true);

        ServicePoint sp = servicePoint();
        List<ServicePoint> homePoints = List.of(sp);

        MedDispatchRec r1 = dispatch(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(22, 0), // 22:00
                2.0,
                false,
                null
        );

        // availability only 09:00-17:00
        List<DroneForServicePoint.Availability> windows = List.of(
                window()
        );

        boolean result = QueryDroneHelper.canHandleAll(
                d,
                windows,
                homePoints,
                List.of(r1),
                1
        );
        assertFalse(result);
    }

    // ---------- MaxCost path via respectsMaxCost ----------

    @Test
    void withMaxCost_costLowEnough_returnsTrue() {
        // costInitial + costFinal + moves * costPerMove must be low enough
        Drone d = drone(10.0, true
                // costPerMove
                // costInitial
                // costFinal
        );

        ServicePoint sp = servicePoint();
        List<ServicePoint> homePoints = List.of(sp);

        // distance is zero (same coordinate) so roundTripMoves ~ 1 (hover)
        MedDispatchRec r1 = dispatch(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(10, 0),
                2.0,
                false,
                5.0    // maxCost high enough
        );

        List<DroneForServicePoint.Availability> windows = List.of(
                window()
        );

        boolean result = QueryDroneHelper.canHandleAll(
                d,
                windows,
                homePoints,
                List.of(r1),
                1
        );
        assertTrue(result);
    }

    @Test
    void withMaxCost_costTooHigh_returnsFalse() {
        Drone d = drone(10.0, true
                // costPerMove
                // costInitial
                // costFinal
        );

        ServicePoint sp = servicePoint();
        List<ServicePoint> homePoints = List.of(sp);

        MedDispatchRec r1 = dispatch(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(10, 0),
                2.0,
                false,
                2.0    // maxCost lower than approx average cost
        );

        List<DroneForServicePoint.Availability> windows = List.of(
                window()
        );

        boolean result = QueryDroneHelper.canHandleAll(
                d,
                windows,
                homePoints,
                List.of(r1),
                1
        );
        assertFalse(result);
    }
}

