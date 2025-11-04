package uk.ac.ed.acp.cw2.service;
import uk.ac.ed.acp.cw2.data.*;

public interface ValidationService {
    boolean isValidCoordinate(Coordinate pos);
    boolean isValidAngle(Double angle);
    boolean isValidRegion(RegionRequest.Region region);
}
