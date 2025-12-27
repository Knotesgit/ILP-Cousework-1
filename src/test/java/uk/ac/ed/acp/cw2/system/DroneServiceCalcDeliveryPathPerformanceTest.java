package uk.ac.ed.acp.cw2.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.external.DroneServiceImpl;
import uk.ac.ed.acp.cw2.external.IlpClientComponent;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * MR-1: End-to-end response time of calcDeliveryPath() must be within 30 seconds.
 *
 * This is an end-to-end performance test of the request pipeline (grouping, assignment,
 * pathfinding, response construction) under a controlled, repeatable workload.
 *
 * Key design points:
 *  - Mock backend for repeatability (no network variance).
 *  - Drone availability MUST be non-empty when dispatch time != null (see QueryDroneHelper.isAvailableAt).
 *  - Geometry is intentionally "unfavourable but solvable":
 *      * restricted areas form vertical wall(s) that block the straight-line route from (0,0)
 *      * a single narrow gap exists but is off-axis (forces detour)
 *      * deliveries are placed behind the wall(s), concentrated near y≈0 to tempt the heuristic
 *
 * If the system returns an empty plan, this is not "fast"; it is a failed request for this dataset.
 */
class DroneServiceCalcDeliveryPathPerformanceTest {

    // ---- MR-1 bound ----
    private static final long LIMIT_NS = 30_000_000_000L; // 30 seconds

    // ---- Coordinate region kept relatively tight so obstacles stay meaningful ----
    private static final double MIN = -0.02;
    private static final double MAX =  0.02;

    // ---- Rectangle size (must be > STEP=0.00015 in GeoUtilities) ----
    private static final double RECT_HALF_W = 0.0016;
    private static final double RECT_HALF_H = 0.0016;

    // ---- Wall parameters ----
    private static final double WALL_X1 = 0.0025;
    private static final double WALL_X2 = 0.0070;     // used in stress
    private static final double GAP_HEIGHT = 0.0060;  // intentionally > ~40*STEP to reduce "grid blocks the gap"
    private static final double GAP_CENTER_Y = 0.012; // off-axis gap

    // ---- Delivery placement behind the wall(s) ----
    private static final double DELIVERY_X_MIN = 0.012;
    private static final double DELIVERY_X_MAX = 0.018;
    private static final double DELIVERY_Y_BAND = 0.0035; // concentrated near y≈0

    @Test
    @DisplayName("MR-1 (Representative): median end-to-end request time within 30 seconds")
    void mr1_representative_medianUnder30s() {
        printWorkloadConfig(
                "Representative",
                30,
                buildRestrictedAreasRepresentative().size()
        );
        warmup(2, this::runRepresentativeOnce);

        long[] samples = measureRunsNs(9, this::runRepresentativeOnce);
        Arrays.sort(samples);

        long median = percentileNearestRank(samples, 50);
        assertTrue(median <= LIMIT_NS,
                "Representative median exceeded 30s: " + nsToMs(median) + " ms; samples_ms=" + nsToMsArray(samples));

        System.out.println("[MR-1 Representative] runs=9 median_ms=" + nsToMs(median) +
                " samples_ms=" + nsToMsArray(samples));
    }

    @Test
    @DisplayName("MR-1 (Controlled stress): p95 end-to-end request time within 30 seconds")
    void mr1_stress_p95Under30s() {
        printWorkloadConfig(
                "Stress",
                70,
                buildRestrictedAreasStress().size()
        );
        warmup(2, this::runStressOnce);

        long[] samples = measureRunsNs(21, this::runStressOnce);
        Arrays.sort(samples);

        long p95 = percentileNearestRank(samples, 95);
        assertTrue(p95 <= LIMIT_NS,
                "Stress p95 exceeded 30s: " + nsToMs(p95) + " ms; samples_ms=" + nsToMsArray(samples));

        System.out.println("[MR-1 Stress] runs=21 p95_ms=" + nsToMs(p95) +
                " samples_ms=" + nsToMsArray(samples));
    }

    /* ---------- One complete request per run (fresh SUT + fresh datasets) ---------- */

    private void runRepresentativeOnce() {
        List<RestrictedArea> ras = buildRestrictedAreasRepresentative();
        DroneServiceImpl service = buildServiceWithMockedBackend(ras);
        List<MedDispatchRec> recs = buildDispatchesRepresentative();
        runOnce(service, recs);
    }

    private void runStressOnce() {
        List<RestrictedArea> ras = buildRestrictedAreasStress();
        DroneServiceImpl service = buildServiceWithMockedBackend(ras);
        List<MedDispatchRec> recs = buildDispatchesStress();
        runOnce(service, recs);
    }

    /**
     * Sanity checks to rule out "trivial fast exits".
     * For this dataset, a valid plan must have >0 moves.
     */
    private static void runOnce(DroneServiceImpl service, List<MedDispatchRec> recs) {
        CalcDeliveryPathResponse resp = service.calcDeliveryPath(recs);
        assertNotNull(resp, "Response must not be null");
        assertNotNull(resp.getDronePaths(), "dronePaths must not be null");
        assertTrue(resp.getTotalMoves() > 0,
                "totalMoves was 0. For this dataset, 0 implies a failed plan / early exit, not a fast success.");
    }

    private static void warmup(int runs, Runnable action) {
        for (int i = 0; i < runs; i++) action.run();
    }

    /* ---------- Mock backend scaffolding (CRITICAL: availability must exist) ---------- */

    private static DroneServiceImpl buildServiceWithMockedBackend(List<RestrictedArea> restrictedAreas) {
        IlpClientComponent ilp = Mockito.mock(IlpClientComponent.class);

        // Single service point at origin.
        ServicePoint sp = new ServicePoint();
        sp.setId(1);
        sp.setName("SP-1");
        sp.setLocation(new Coordinate(0.0, 0.0));

        // Single drone with permissive limits so the workload exercises planning rather than rejection.
        Drone drone = new Drone();
        drone.setId("DR-1");
        drone.setName("Drone-1");

        Drone.DroneCapability cap = new Drone.DroneCapability();
        cap.setCapacity(1_000_000.0);
        cap.setCooling(false);
        cap.setHeating(false);
        cap.setMaxMoves(10_000_000);
        cap.setCostPerMove(0.01);
        cap.setCostInitial(0.0);
        cap.setCostFinal(0.0);
        drone.setCapability(cap);

        // DRONE AVAILABILITY:
        // Therefore we must provide time windows covering all days and the full day.
        List<DroneForServicePoint.Availability> allWeekAllDay = buildAllWeekAllDayAvailability();

        DroneForServicePoint.Item item = new DroneForServicePoint.Item();
        item.setId("DR-1");
        item.setAvailability(allWeekAllDay);

        DroneForServicePoint dfsp = new DroneForServicePoint();
        dfsp.setServicePointId(1);
        dfsp.setDrones(new ArrayList<>(List.of(item)));

        when(ilp.getAllDrones()).thenReturn(new ArrayList<>(List.of(drone)));
        when(ilp.getServicePoints()).thenReturn(new ArrayList<>(List.of(sp)));
        when(ilp.getDronesForServicePoints()).thenReturn(new ArrayList<>(List.of(dfsp)));

        // Provide a fresh list each time in case the SUT mutates it.
        when(ilp.getRestrictedAreas()).thenAnswer(inv -> new ArrayList<>(restrictedAreas));

        return new DroneServiceImpl(ilp);
    }

    private static List<DroneForServicePoint.Availability> buildAllWeekAllDayAvailability() {
        ArrayList<DroneForServicePoint.Availability> out = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            // LocalTime.parse accepts "HH:mm" in your code path.
            out.add(new DroneForServicePoint.Availability(d.name(), "00:00", "23:59"));
        }
        return out;
    }

    /* ---------- Dispatch dataset (structured, not "random easy") ---------- */

    private static List<MedDispatchRec> buildDispatchesRepresentative() {
        return buildDispatchesStructured(30, 12345);
    }

    private static List<MedDispatchRec> buildDispatchesStress() {
        return buildDispatchesStructured(70, 54321);
    }

    /**
     * Dispatches are intentionally structured:
     *  - date/time always non-null => orderedDays non-empty => main planning loop executes
     *  - deliveries behind wall(s), concentrated near y≈0 => heuristic initially prefers blocked route
     */
    private static List<MedDispatchRec> buildDispatchesStructured(int count, long seed) {
        ArrayList<MedDispatchRec> out = new ArrayList<>(count);
        Random rnd = new Random(seed);

        final LocalDate START_DATE = LocalDate.of(2030, 1, 1);
        final int DAYS = 14;

        // Reduce measurement noise while still varying time across the day.
        final int TIME_STEP_MIN = 15;

        for (int i = 0; i < count; i++) {
            LocalDate date = START_DATE.plusDays(rnd.nextInt(DAYS));
            int slots = 1440 / TIME_STEP_MIN;
            int minuteOfDay = rnd.nextInt(slots) * TIME_STEP_MIN;
            LocalTime time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60);

            double lng = rand(rnd, DELIVERY_X_MIN, DELIVERY_X_MAX);
            double lat = rand(rnd, -DELIVERY_Y_BAND, DELIVERY_Y_BAND);

            out.add(dispatchWithDateTime(2000 + i, date, time, 0.5, new Coordinate(lng, lat)));
        }
        return out;
    }

    private static MedDispatchRec dispatchWithDateTime(int id, LocalDate date, LocalTime time,
                                                       double capacity, Coordinate delivery) {
        MedDispatchRec r = new MedDispatchRec();
        r.setId(id);
        r.setDate(date);
        r.setTime(time);
        r.setDelivery(delivery);

        MedDispatchRec.Requirement req = new MedDispatchRec.Requirement();
        req.setCapacity(capacity);
        req.setCooling(false);
        req.setHeating(false);
        req.setMaxCost(null);
        r.setRequirements(req);

        return r;
    }

    /* ---------- Restricted areas: solvable but unfavourable "wall(s) with gap" ---------- */

    private static List<RestrictedArea> buildRestrictedAreasRepresentative() {
        return buildWallsDataset(1, 999);
    }

    private static List<RestrictedArea> buildRestrictedAreasStress() {
        return buildWallsDataset(2, 777);
    }

    private static List<RestrictedArea> buildWallsDataset(int wallCount, long seed) {
        Random rnd = new Random(seed);
        ArrayList<RestrictedArea> out = new ArrayList<>();

        // Slight deterministic jitter to avoid brittle tuning; still reproducible by seed.
        double gapCenterY = clamp(GAP_CENTER_Y + rand(rnd, -0.001, 0.001),
                MIN + GAP_HEIGHT, MAX - GAP_HEIGHT);

        out.addAll(buildVerticalWall(WALL_X1, gapCenterY, 0));
        if (wallCount >= 2) {
            out.addAll(buildVerticalWall(WALL_X2, gapCenterY * 0.85, out.size()));
        }
        return out;
    }

    /**
     * Wall = stacked rectangles along Y, leaving a single gap (passable corridor).
     * No rejection sampling => generation cannot get stuck.
     */
    private static List<RestrictedArea> buildVerticalWall(double wallX, double gapCenterY, int idBase) {
        ArrayList<RestrictedArea> wall = new ArrayList<>();

        final double gapLo = gapCenterY - GAP_HEIGHT / 2.0;
        final double gapHi = gapCenterY + GAP_HEIGHT / 2.0;

        // Spacing between rectangle centres along Y (avoid overlap).
        final double stepY = 2 * RECT_HALF_H + 0.0004;

        int id = idBase;
        for (double cy = MIN + RECT_HALF_H; cy <= MAX - RECT_HALF_H + 1e-12; cy += stepY) {
            // Leave the gap open.
            if (cy >= gapLo && cy <= gapHi) continue;
            wall.add(buildRect("RA-WALL-" + wallX + "-" + id, id, wallX, cy));
            id++;
        }

        // Strengthen the block near the direct route.
        wall.add(buildRect("RA-CAP-" + wallX, id, wallX, 0.0));

        return wall;
    }

    private static RestrictedArea buildRect(String name, int id, double cx, double cy) {
        RestrictedArea ra = new RestrictedArea();
        ra.setId(id);
        ra.setName(name);

        ArrayList<Coordinate> rect = new ArrayList<>(5);
        rect.add(new Coordinate(cx - RECT_HALF_W, cy - RECT_HALF_H));
        rect.add(new Coordinate(cx + RECT_HALF_W, cy - RECT_HALF_H));
        rect.add(new Coordinate(cx + RECT_HALF_W, cy + RECT_HALF_H));
        rect.add(new Coordinate(cx - RECT_HALF_W, cy + RECT_HALF_H));
        rect.add(new Coordinate(cx - RECT_HALF_W, cy - RECT_HALF_H));
        ra.setVertices(rect);

        return ra;
    }

    /* ---------- Measurement utilities ---------- */

    private static void printWorkloadConfig(
            String label,
            int dispatchCount,
            int restrictedAreaCount
    ) {
        System.out.println(
                "[MR-1 Config " + label + "] " +
                        "dispatchCount=" + dispatchCount +
                        ", restrictedAreaCount=" + restrictedAreaCount +
                        ", MIN=" + MIN +
                        ", MAX=" + MAX +
                        ", WALL_X1=" + WALL_X1 +
                        ", WALL_X2=" + WALL_X2 +
                        ", GAP_CENTER_Y=" + GAP_CENTER_Y +
                        ", GAP_HEIGHT=" + GAP_HEIGHT
        );
    }

    private static long[] measureRunsNs(int runs, Runnable action) {
        long[] out = new long[runs];
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            action.run();
            out[i] = System.nanoTime() - start;
        }
        return out;
    }

    private static long percentileNearestRank(long[] sorted, int p) {
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }

    private static double rand(Random rnd, double min, double max) {
        return min + (max - min) * rnd.nextDouble();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static long nsToMs(long ns) {
        return ns / 1_000_000;
    }

    private static String nsToMsArray(long[] ns) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(nsToMs(ns[i]));
        }
        sb.append("]");
        return sb.toString();
    }
}

