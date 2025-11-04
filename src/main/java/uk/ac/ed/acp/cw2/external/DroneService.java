package uk.ac.ed.acp.cw2.external;
import uk.ac.ed.acp.cw2.data.Drone;

import java.util.List;

public interface DroneService {
    List<Integer> getDronesWithCooling(boolean state);
    Drone getDroneDetails(int id);

    List<Integer> getDronesByAttribute(String attribute, String value);
}
