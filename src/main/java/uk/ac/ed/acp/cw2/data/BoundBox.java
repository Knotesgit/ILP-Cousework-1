package uk.ac.ed.acp.cw2.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// Represent a buffer zone alerting for a restricted area
@Getter
@Setter

public class BoundBox {
    Coordinate max;
    Coordinate min;

    public BoundBox(Coordinate max, Coordinate min) {
        this.max = max;
        this.min = min;
    }
}
