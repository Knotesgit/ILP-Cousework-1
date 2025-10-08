package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

// Request body model for endpoints that take a start position and an angle (nextPosition).
@Getter
@Setter
public class NextPositionRequest {
    private Coordinate start;
    private Double angle;

}
