package uk.ac.ed.acp.cw2.data;

import java.util.List;

// Data holder for a region (name, coordinates).
public class Region {
    private String name;
    private List<Coordinate> vertices;

    public String getName() {
        return name;
    }

    public List<Coordinate> getVertices() {
        return vertices;
    }

    public void setVertices(List<Coordinate> vertices) {
        this.vertices = vertices;
    }

    public void setName(String name) {
        this.name = name;
    }
}
