package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

//Request body model for endpoints that take a position and a region (nextPosition).
@Getter
@Setter
public class RegionRequest {
    private Coordinate position;
    private Region region;

    // Data holder for a region (name, coordinates).
    @Getter
    @Setter
    public static class Region {
        private String name;
        private List<Coordinate> vertices;

    }
}

