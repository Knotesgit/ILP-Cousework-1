package uk.ac.ed.acp.cw2.data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Represents a service point from which drones can depart and return.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicePoint {
    private String name;
    private int id;
    private Coordinate location;

}
