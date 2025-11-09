package uk.ac.ed.acp.cw2.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

// Represents a restricted (no-fly) area in the ILP dataset.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestrictedArea {
    private String name;
    private Integer id;
    private Limits limits;
    private List<Coordinate> vertices;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Limits {
        private int lower;
        private int upper;
    }
}
