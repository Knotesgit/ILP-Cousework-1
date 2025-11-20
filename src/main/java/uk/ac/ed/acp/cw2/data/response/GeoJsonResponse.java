package uk.ac.ed.acp.cw2.data.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

// Represents a single GeoJSON Feature
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoJsonResponse {
    private String type = "Feature";
    private Properties properties;
    private Geometry geometry;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Properties {
        private String droneId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Geometry {
        private String type = "LineString";
        private List<List<Double>> coordinates; // [[lng, lat], ...]
    }
}

