package uk.ac.ed.acp.cw2.service;
import uk.ac.ed.acp.cw2.data.*;

import java.util.List;
public interface ValidationService {
    boolean isValidCoordinate(Coordinate pos);
    boolean isValidAngle(Double angle);
    boolean isValidRegion(Region region);
}
