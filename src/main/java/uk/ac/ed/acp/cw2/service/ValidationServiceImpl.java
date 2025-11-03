package uk.ac.ed.acp.cw2.service;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.Region;

import java.util.List;

@Service
public class ValidationServiceImpl implements ValidationService {

    // Numerical epsilon for floating-point comparisons only.
    private static final double EPSILON = 1e-12;
    // Checks whether a coordinate is non-null and within valid lat/lng ranges
    @Override
    public boolean isValidCoordinate(Coordinate pos) {
        if (pos == null) return false;
        return  pos.getLat() != null && pos.getLng() != null &&
                !Double.isNaN(pos.getLat()) && !Double.isNaN(pos.getLng()) &&
                Double.isFinite(pos.getLat()) && Double.isFinite(pos.getLng())&&
                pos.getLng() >=-180 && pos.getLng() <= 180 &&
                pos.getLat()>= -90 && pos.getLat()<=90;
    }

    // Checks whether an angle is non-null and finite.
    @Override
    public boolean isValidAngle(Double angle) {
        if (angle == null || Double.isNaN(angle) || !Double.isFinite(angle)) return false;
        if (angle < 0 || angle > 360) return false;
        double remainder = angle % 22.5;
        return Math.abs(remainder) < EPSILON || Math.abs(remainder - 22.5) < EPSILON;
    }

    // Checks whether a region is valid
    @Override
    public boolean isValidRegion(Region region) {
        if (region == null) return false;
        List<Coordinate> v = region.getVertices();
        if (v == null || v.size() < 4) return false;
        if (!isClosed(v)) return false;
        for (Coordinate c : v) if (!isValidCoordinate(c)) return false;
        return true;
    }

    // Check whether the region(polygon) is closed
    public boolean isClosed(List<Coordinate> v) {
        if (v.size() < 2) return false;
        Coordinate a = v.get(0), b = v.get(v.size()-1);
        return Math.abs(a.getLng() - b.getLng()) <= EPSILON &&
                Math.abs(a.getLat() - b.getLat()) <= EPSILON;
    }
}
