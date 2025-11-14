package uk.ac.ed.acp.cw2.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Drone data model representing a single drone and its capabilities.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Drone {
    private String id;
    private String name;
    private DroneCapability capability;

    // Data model representing the operational capabilities of a drone.
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DroneCapability {
        private boolean cooling;
        private boolean heating;
        private double capacity;
        private int maxMoves;
        private double costPerMove;
        private double costInitial;
        private double costFinal;
    }
}
