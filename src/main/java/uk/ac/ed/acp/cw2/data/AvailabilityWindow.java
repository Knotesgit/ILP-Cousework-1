package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
@Getter
@Setter
public class AvailabilityWindow {
    private DayOfWeek dayOfWeek;
    private LocalTime from;
    private LocalTime until;
    public AvailabilityWindow(DayOfWeek dayOfWeek, LocalTime from, LocalTime until) {
        this.dayOfWeek = dayOfWeek;
        this.from = from;
        this.until = until;
    }
    public boolean includes(LocalTime time) {
        return !time.isBefore(from) && !time.isAfter(until);
    }

}
