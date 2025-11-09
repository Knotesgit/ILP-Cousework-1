package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FlightBuilder {
    final int droneId;
    final ServicePoint servicePoint;
    final double capacity;
    final int maxMoves;
    final double costPerMove, costInitial, costFinal;

    final LocalDate flightDate;
    final DayOfWeek flightDOW;
    final LocalTime flightStart;

    // Status
    int stepsUsed = 0;
    //int reservedReturnSteps = 0;
    Coordinate end;          // Current end
    double currentLoad = 0;
    boolean hasCooling = false, hasHeating = false;

    //
    int deliveryCount = 0;
    List<Double> existingMaxCosts = new ArrayList<>();

    //
    List<CalcDeliveryPathResponse.DeliverySegment> segments = new ArrayList<>();

    public FlightBuilder(int droneId, ServicePoint sp, double capacity, int maxMoves,
                         double cpm, double ci, double cf,
                         MedDispatchRec first) {
        this.droneId = droneId;
        this.servicePoint = sp;
        this.capacity = capacity;
        this.maxMoves = maxMoves;
        this.costPerMove = cpm;
        this.costInitial = ci;
        this.costFinal = cf;

        this.end = sp.getLocation();

        this.flightDate = first.getDate();
        this.flightDOW  = this.flightDate.getDayOfWeek();
        this.flightStart = first.getTime();
    }

    public void addSegment(int deliveryId, List<Coordinate> path, int steps, double addedLoad, Double maxCost,
                    boolean markCooling, boolean markHeating) {
        var seg = new CalcDeliveryPathResponse.DeliverySegment();
        seg.setDeliveryId(deliveryId);
        seg.setFlightPath(path);
        segments.add(seg);

        this.stepsUsed += steps;
        this.end = path.getLast();
        this.currentLoad += addedLoad;
        if(maxCost!=null)
            existingMaxCosts.add(maxCost);
        this.deliveryCount += 1;
        this.hasCooling  = this.hasCooling  || markCooling;
        this.hasHeating  = this.hasHeating  || markHeating;
    }

    public void addReturn(List<Coordinate> back, int steps) {
        if (!segments.isEmpty()) {
            var lastPath = segments.getLast().getFlightPath();
            for (int i = 1; i < back.size(); i++) lastPath.add(back.get(i));
        }
        this.stepsUsed += steps;
        this.end = back.getLast();
    }
}