package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class DroneForServicePoint {
    private int servicePointId;
    private List<Item> drones;

    @Getter
    @Setter
    public static class Item {
        private int id; // droneId
        private List<Availability> availability;
    }

    @Getter
    @Setter
    public static class Availability {
        private String dayOfWeek;
        private String from;
        private String until;
    }
}
