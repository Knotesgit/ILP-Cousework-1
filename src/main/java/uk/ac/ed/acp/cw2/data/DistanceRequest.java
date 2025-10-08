package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

// Request body model for endpoints that take two positions (distanceTo, isCloseTo).
@Getter
@Setter
public class DistanceRequest
{
    private Coordinate position1;
    private Coordinate position2;

}
