package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

//Mutable builder object used during delivery path planning.
@Getter
@Setter
public class FlightBuilder {
    final String droneId;
    final ServicePoint servicePoint;
    final double capacity;
    final int maxMoves;
    final double costPerMove, costInitial, costFinal;

    final LocalDate flightDate;
    final DayOfWeek flightDOW;
    final LocalTime flightStart;

    // Status
    int stepsUsed = 0;
    Coordinate end;          // Current end
    double currentLoad = 0;
    boolean hasCooling = false, hasHeating = false;

    // Number of deliveries included so far
    int deliveryCount = 0;
    List<Double> existingMaxCosts = new ArrayList<>();

    // Ordered list of path segments representing each delivery
    List<CalcDeliveryPathResponse.DeliverySegment> segments = new ArrayList<>();

    // Construct a new builder for a drone’s initial flight plan.
    public FlightBuilder(String droneId, ServicePoint sp, double capacity, int maxMoves,
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
        if(flightDate!=null)
            this.flightDOW  = this.flightDate.getDayOfWeek();
        else
            this.flightDOW  = null;
        this.flightStart = first.getTime();
    }

    // Append a new delivery segment to this flight plan.
    // Updates step count, position, capacity, and requirement flags.
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

    // Attach a return path to the last delivery segment, representing
    // the drone’s journey back to the service point.
    public void addReturn(List<Coordinate> back, int steps) {
        if (!segments.isEmpty()) {
            var seg = new CalcDeliveryPathResponse.DeliverySegment();
            seg.setDeliveryId(null);
            seg.setFlightPath(back);
            segments.add(seg);
            // var lastPath = segments.getLast().getFlightPath();
            // for (int i = 1; i < back.size(); i++) lastPath.add(back.get(i));
        }
        this.stepsUsed += steps;
        this.end = back.getLast();
    }
}