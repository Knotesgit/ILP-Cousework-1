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
public class RestrictedArea {
    private String name;
    private Integer id;
    private Limits limits;     // 缺省表示 no-fly (lower=0, upper=-1)
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
