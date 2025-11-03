package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

// Drone data model representing a single drone and its capabilities.
@Getter
@Setter
public class Drone {
    private int id;
    private String name;
    private DroneCapability capability;
}
