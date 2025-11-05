package uk.ac.ed.acp.cw2.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalcDeliveryPathResponse {
    private double totalCost;
    private int totalMoves;
    private List<DronePath> dronePaths;


    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DronePath {
        private int droneId;
        private List<DeliverySegment> deliveries;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeliverySegment {
        private int deliveryId;
        private List<Coordinate> flightPath;
    }
}
