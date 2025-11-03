package uk.ac.ed.acp.cw2.data;
import lombok.Getter;
import lombok.Setter;

// Data model representing the operational capabilities of a drone.
@Getter
@Setter
public class DroneCapability {
    private boolean cooling;
    private boolean heating;
    private double capacity;
    private int maxMoves;
    private double costPerMove;
    private double costInitial;
    private double costFinal;
}
