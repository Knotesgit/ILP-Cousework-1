package uk.ac.ed.acp.cw2.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

// Represents a medical dispatch record containing delivery requirements.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedDispatchRec {
    private Integer id;
    private LocalDate date;
    private LocalTime time;
    private Requirement requirements;
    private Coordinate delivery;


    // Represents delivery requirements for a medical dispatch.
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Requirement {
        private Double capacity;
        private boolean cooling; // optional
        private boolean heating; // optional
        private Double maxCost;  // optional
    }
}
