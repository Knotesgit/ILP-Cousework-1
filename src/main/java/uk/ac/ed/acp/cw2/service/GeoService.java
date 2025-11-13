package uk.ac.ed.acp.cw2.service;

import uk.ac.ed.acp.cw2.data.*;

import java.util.List;

public interface GeoService {
    boolean isNear(Coordinate pos1, Coordinate pos2);
    boolean onSegment(Coordinate a ,Coordinate p ,Coordinate q);
    boolean isPointInRegion(Coordinate p, List<Coordinate> vertices);
    double distanceBetween(Coordinate pos1, Coordinate pos2);
    Coordinate nextPosition(Coordinate start, double angle);
}
