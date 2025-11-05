package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

// Represents available drones for a specific service point.
@Getter
@Setter
public class DroneForServicePoint {
    private int servicePointId;
    private List<Item> drones;

    // Represents a drone assigned to a service point.
    @Getter
    @Setter
    public static class Item {
        private int id; // droneId
        private List<Availability> availability;
    }

    // Represents a drone's availability time slot.
    @Getter
    @Setter
    public static class Availability {
        private String dayOfWeek;
        private String from;
        private String until;
    }
}
