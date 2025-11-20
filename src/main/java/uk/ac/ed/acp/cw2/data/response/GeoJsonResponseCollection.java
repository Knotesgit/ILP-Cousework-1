package uk.ac.ed.acp.cw2.data.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Response model for /api/v1/calcDeliveryPathAsGeoJson
// Represents multiple GeoJSON Feature
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoJsonResponseCollection {
    private String type = "FeatureCollection";
    private List<GeoJsonResponse> features;
}
