package uk.ac.ed.acp.cw2.service;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.*;
import uk.ac.ed.acp.cw2.utility.GeoUtilities;
import uk.ac.ed.acp.cw2.utility.PathFindingHelper;

import java.util.*;


@Service
public class GeoServiceImpl implements GeoService {

    // Check whether two pos are near (distance < 0.00015)
    @Override
    public boolean isNear(Coordinate pos1, Coordinate pos2) {
        return GeoUtilities.isNear(pos1,pos2);
    }

    // Check whether the point a is on segment pq (inclusive)
    @Override
    public boolean onSegment(Coordinate a, Coordinate p, Coordinate q) {
       return GeoUtilities.onSegment(a,p,q);
    }

    // Check whether a point is in a region
    @Override
    public boolean isPointInRegion(Coordinate p, List<Coordinate> vertices) {
       return GeoUtilities.isPointInRegion(p, vertices);
    }

    // Computes Euclidean distance between two coordinates
    @Override
    public double distanceBetween(Coordinate pos1, Coordinate pos2) {
        return GeoUtilities.distanceBetween(pos1,pos2);
    }

    // Compute the next coordinate from a start position and an angle.
   @Override
   public Coordinate nextPosition(Coordinate start, double angle){
       return GeoUtilities.nextPosition(start,angle);
   }
}
