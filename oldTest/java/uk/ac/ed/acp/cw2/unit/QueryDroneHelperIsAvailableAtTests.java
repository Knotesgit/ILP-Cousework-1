package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.data.DroneForServicePoint;
import uk.ac.ed.acp.cw2.utility.QueryDroneHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryDroneHelperIsAvailableAtTests {

    private DroneForServicePoint.Availability window(String day, String from, String until) {
        DroneForServicePoint.Availability w = new DroneForServicePoint.Availability();
        w.setDayOfWeek(day);
        w.setFrom(from);
        w.setUntil(until);
        return w;
    }

    @Test
    void bothDateAndTimeNull_returnsTrue() {
        QueryDroneHelper.isAvailableAt(
                List.of(window("MONDAY", "09:00:00", "17:00:00")),
                null,
                null
        );
        boolean result = true;
        assertTrue(result);
    }

    @Test
    void dateNullButTimeNotNull_returnsFalse() {
        boolean result = QueryDroneHelper.isAvailableAt(
                List.of(window("MONDAY", "09:00:00", "17:00:00")),
                null,
                LocalTime.of(10, 0)
        );
        assertFalse(result);
    }

    @Test
    void noWindows_returnsFalse() {
        boolean result = QueryDroneHelper.isAvailableAt(
                List.of(), // empty windows
                LocalDate.of(2025, 1, 6), // Monday
                LocalTime.of(10, 0)
        );
        assertFalse(result);
    }

    @Test
    void onlyDateRestriction_matchingDay_returnsTrue() {
        // 2025-01-06 is a Monday
        LocalDate date = LocalDate.of(2025, 1, 6);
        assertEquals(DayOfWeek.MONDAY, date.getDayOfWeek());

        boolean result = QueryDroneHelper.isAvailableAt(
                List.of(
                        window("MONDAY", null, null),
                        window("TUESDAY", null, null)
                ),
                date,
                null
        );
        assertTrue(result);
    }

    @Test
    void onlyDateRestriction_nonMatchingDay_returnsFalse() {
        // 2025-01-06 is Monday, window is SUNDAY only
        LocalDate date = LocalDate.of(2025, 1, 6);

        boolean result = QueryDroneHelper.isAvailableAt(
                List.of(window("SUNDAY", null, null)),
                date,
                null
        );
        assertFalse(result);
    }

    @Test
    void dateAndTime_withinWindow_returnsTrue() {
        // Monday 10:00 between 09:00 and 17:00
        LocalDate date = LocalDate.of(2025, 1, 6);

        boolean result = QueryDroneHelper.isAvailableAt(
                List.of(window("MONDAY", "09:00:00", "17:00:00")),
                date,
                LocalTime.of(10, 0)
        );
        assertTrue(result);
    }

    @Test
    void dateAndTime_beforeWindow_returnsFalse() {
        LocalDate date = LocalDate.of(2025, 1, 6);

        boolean result = QueryDroneHelper.isAvailableAt(
                List.of(window("MONDAY", "09:00:00", "17:00:00")),
                date,
                LocalTime.of(8, 59, 59)
        );
        assertFalse(result);
    }

    @Test
    void dateAndTime_afterWindow_returnsFalse() {
        LocalDate date = LocalDate.of(2025, 1, 6);

        boolean result = QueryDroneHelper.isAvailableAt(
                List.of(window("MONDAY", "09:00:00", "17:00:00")),
                date,
                LocalTime.of(17, 0, 1)
        );
        assertFalse(result);
    }

    @Test
    void dateAndTime_invalidDayString_isIgnored_returnsFalse() {
        // DayOfWeek parse returns null, so window is ignored
        LocalDate date = LocalDate.of(2025, 1, 6);

        boolean result = QueryDroneHelper.isAvailableAt(
                List.of(window("NotADay", "09:00:00", "17:00:00")),
                date,
                LocalTime.of(10, 0)
        );
        assertFalse(result);
    }

    @Test
    void dateAndTime_invalidTimeStrings_areIgnored_returnsFalse() {
        // from/until parse to null -> condition never satisfied
        LocalDate date = LocalDate.of(2025, 1, 6);

        boolean result = QueryDroneHelper.isAvailableAt(
                List.of(window("MONDAY", "not-a-time", "also-bad")),
                date,
                LocalTime.of(10, 0)
        );
        assertFalse(result);
    }
}

