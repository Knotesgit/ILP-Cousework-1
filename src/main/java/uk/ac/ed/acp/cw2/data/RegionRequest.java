package uk.ac.ed.acp.cw2.data;

//Request body model for endpoints that take a position and a region (nextPosition).
public class RegionRequest {
    private Coordinate position;
    private Region region;

    public Coordinate getPosition() {
        return position;
    }

    public void setPosition(Coordinate position) {
        this.position = position;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }
}

