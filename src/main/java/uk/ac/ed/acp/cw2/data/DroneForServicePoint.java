package uk.ac.ed.acp.cw2.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private int id; // droneId
        private List<Availability> availability;
    }

    // Represents a drone's availability time slot.
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    public static class Availability {
        private String dayOfWeek;
        private String from;
        private String until;
    }
}
