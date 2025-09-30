package uk.ac.ed.acp.cw2.data;

// Request body model for endpoints that take a start position and an angle (nextPosition).
public class NextPositionRequest {
    private Coordinate start;
    private Double angle;
    public Coordinate getStart() { return start; }
    public void setStart(Coordinate start) { this.start = start; }
    public Double getAngle() { return angle; }
    public void setAngle(Double angle) { this.angle = angle; }
}
