package uk.ac.ed.acp.cw2.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.ac.ed.acp.cw2.data.Coordinate;

import java.util.List;

// Response model for the /calcDeliveryPath endpoint.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalcDeliveryPathResponse {
    private double totalCost;
    private int totalMoves;
    private List<DronePath> dronePaths;

    //Represents the entire flight plan of a single drone.
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DronePath {
        private int droneId;
        private List<DeliverySegment> deliveries;
    }

    //Represents a single delivery within a drone's route.
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeliverySegment {
        private Integer deliveryId;
        private List<Coordinate> flightPath;
    }
}
