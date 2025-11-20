package uk.ac.ed.acp.cw2.external;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.data.response.GeoJsonResponseCollection;

import java.util.List;

public interface DroneService {
    List<String> dronesWithCooling(Boolean state);
    Drone droneDetails(String id);
    List<String> queryAsPath(String attribute, String value);
    List<String> query(List<QueryCondition> conditions);
    List<String> queryAvailableDrones(List<MedDispatchRec> dispatches);
    CalcDeliveryPathResponse calcDeliveryPath(List<MedDispatchRec> dispatches);
    GeoJsonResponseCollection calcDeliveryPathAsGeoJson(List<MedDispatchRec> dispatches);
}
