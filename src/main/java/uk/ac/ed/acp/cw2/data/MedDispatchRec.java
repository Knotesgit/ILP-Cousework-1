package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

// Represents a medical dispatch record containing delivery requirements.
@Getter
@Setter
public class MedDispatchRec {
    private int id;
    private LocalDate date;
    private LocalTime time;
    private Requirement requirements;

    // Represents delivery requirements for a medical dispatch.
    @Getter
    @Setter
    public static class Requirement {
        private double capacity;
        private boolean cooling;
        private boolean heating;
        private Double maxCost;
    }
}
