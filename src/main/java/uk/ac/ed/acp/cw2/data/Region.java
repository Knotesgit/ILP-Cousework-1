package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Data holder for a region (name, coordinates).
@Getter
@Setter
public class Region {
    private String name;
    private List<Coordinate> vertices;

}
