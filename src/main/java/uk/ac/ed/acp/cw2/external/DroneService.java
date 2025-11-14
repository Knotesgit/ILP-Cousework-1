package uk.ac.ed.acp.cw2.external;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.data.response.GeoJsonResponse;

import java.util.List;

public interface DroneService {
    List<String> getDronesWithCooling(Boolean state);
    Drone getDroneDetails(String id);
    List<String> getDronesByAttribute(String attribute, String value);
    List<String> queryByAttributes(List<QueryCondition> conditions);
    List<String> queryAvailableDrones(List<MedDispatchRec> dispatches);
    CalcDeliveryPathResponse calcDeliveryPath(List<MedDispatchRec> dispatches);
    GeoJsonResponse  calcDeliveryPathAsGeoJson(List<MedDispatchRec> dispatches);
}
