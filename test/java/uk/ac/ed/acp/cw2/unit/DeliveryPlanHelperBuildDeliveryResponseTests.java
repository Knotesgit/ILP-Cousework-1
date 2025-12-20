package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.FlightBuilder;
import uk.ac.ed.acp.cw2.data.ServicePoint;
import uk.ac.ed.acp.cw2.data.MedDispatchRec;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.utility.DeliveryPlanHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeliveryPlanHelper.buildDeliveryResponse and emptyDeliveryResponse.
 * Goals:
 *  - emptyDeliveryResponse always returns zero cost / zero moves / empty dronePaths.
 *  - buildDeliveryResponse on null or empty list falls back to emptyDeliveryResponse.
 *  - buildDeliveryResponse correctly aggregates cost and moves from FlightBuilder.
 *  - Delivery segments are copied (not shared by reference) and preserve deliveryId and path.
 */
class DeliveryPlanHelperBuildDeliveryResponseTests {

    // Small helpers to build objects used by FlightBuilder

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

    private MedDispatchRec dispatch(int id, LocalDate date, LocalTime time) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(id);
        rec.setDate(date);
        rec.setTime(time);
        return rec;
    }

    @Test
    void emptyDeliveryResponse_hasZeroCostZeroMovesAndNoDronePaths() {
        CalcDeliveryPathResponse res = DeliveryPlanHelper.emptyDeliveryResponse();

        assertNotNull(res);
        assertEquals(0.0, res.getTotalCost(), 1e-12);
        assertEquals(0, res.getTotalMoves());
        assertNotNull(res.getDronePaths());
        assertTrue(res.getDronePaths().isEmpty());
    }

    @Test
    void buildDeliveryResponse_nullOrEmptyFlights_returnsEmptyResponse() {
        CalcDeliveryPathResponse resNull = DeliveryPlanHelper.buildDeliveryResponse(null);
        CalcDeliveryPathResponse resEmpty = DeliveryPlanHelper.buildDeliveryResponse(List.of());

        // Both must behave like emptyDeliveryResponse
        for (CalcDeliveryPathResponse res : List.of(resNull, resEmpty)) {
            assertNotNull(res);
            assertEquals(0.0, res.getTotalCost(), 1e-12);
            assertEquals(0, res.getTotalMoves());
            assertNotNull(res.getDronePaths());
            assertTrue(res.getDronePaths().isEmpty());
        }
    }

    @Test
    void buildDeliveryResponse_singleFlight_singleSegment_costAndMovesCorrect() {
        // Build a simple FlightBuilder:
        //  - one drone D1
        //  - one service point at (0,0)
        //  - capacity, maxMoves arbitrarily large
        //  - costPerMove = 2.0, costInitial = 1.0, costFinal = 3.0
        //  - one delivery segment of 4 steps.
        ServicePoint sp = servicePoint(1, 0.0, 0.0);
        MedDispatchRec first = dispatch(42, LocalDate.of(2025, 1, 1), LocalTime.of(10, 0));

        double capacity = 100.0;
        int maxMoves = 1000;
        double cpm = 2.0;
        double ci = 1.0;
        double cf = 3.0;

        FlightBuilder fb = new FlightBuilder(
                "D1", sp, capacity, maxMoves,
                cpm, ci, cf, first
        );

        // Path with 5 points = 4 steps
        List<Coordinate> path = List.of(
                coord(0.0, 0.0),
                coord(0.001, 0.0),
                coord(0.002, 0.0),
                coord(0.003, 0.0),
                coord(0.004, 0.0)
        );
        int steps = path.size() - 1; // 4
        fb.addSegment(
                42,
                path,
                steps,
                10.0,   // addedLoad (not relevant for cost)
                null,   // maxCost
                false,
                false
        );

        CalcDeliveryPathResponse res =
                DeliveryPlanHelper.buildDeliveryResponse(List.of(fb));

        // cost formula: ci + cf + stepsUsed * cpm = 1 + 3 + 4 * 2 = 12
        assertEquals(12.0, res.getTotalCost(), 1e-12);
        assertEquals(4, res.getTotalMoves());

        assertNotNull(res.getDronePaths());
        assertEquals(1, res.getDronePaths().size());

        var dp = res.getDronePaths().get(0);
        assertEquals("D1", dp.getDroneId());
        assertNotNull(dp.getDeliveries());
        assertEquals(1, dp.getDeliveries().size());

        var segOut = dp.getDeliveries().get(0);
        assertEquals(42, segOut.getDeliveryId());
        assertNotNull(segOut.getFlightPath());
        assertEquals(path.size(), segOut.getFlightPath().size());
    }

    @Test
    void buildDeliveryResponse_multipleFlights_aggregatesCostAndMoves() {
        ServicePoint sp1 = servicePoint(1, 0.0, 0.0);
        ServicePoint sp2 = servicePoint(2, 1.0, 1.0);

        MedDispatchRec first1 = dispatch(1,
                LocalDate.of(2025, 1, 1), LocalTime.of(9, 0));
        MedDispatchRec first2 = dispatch(2,
                LocalDate.of(2025, 1, 2), LocalTime.of(10, 0));

        // Flight 1: D1 with stepsUsed = 3, cpm=1, ci=1, cf=1  => cost = 1+1+3*1 = 5
        FlightBuilder fb1 = new FlightBuilder(
                "D1", sp1, 100.0, 100,
                1.0, 1.0, 1.0, first1
        );
        fb1.addSegment(1,
                List.of(coord(0, 0),
                        coord(1, 0),
                        coord(2, 0),
                        coord(3, 0)), 3,
                5.0, null, false, false);

        // Flight 2: D2 with stepsUsed = 2, cpm=2, ci=0, cf=2  => cost = 0+2+2*2 = 6
        FlightBuilder fb2 = new FlightBuilder(
                "D2", sp2, 100.0, 100,
                2.0, 0.0, 2.0, first2
        );
        fb2.addSegment(2,
                List.of(coord(1, 1),
                        coord(2, 1),
                        coord(3, 1)),
                2, 3.0, null, false, false);

        CalcDeliveryPathResponse res =
                DeliveryPlanHelper.buildDeliveryResponse(List.of(fb1, fb2));

        // Total cost = 5 + 6 = 11
        assertEquals(11.0, res.getTotalCost(), 1e-12);
        // Total moves = 3 + 2 = 5
        assertEquals(5, res.getTotalMoves());

        assertNotNull(res.getDronePaths());
        assertEquals(2, res.getDronePaths().size());

        // Order is the same as input
        var dp1 = res.getDronePaths().get(0);
        var dp2 = res.getDronePaths().get(1);

        assertEquals("D1", dp1.getDroneId());
        assertEquals("D2", dp2.getDroneId());
    }
}
