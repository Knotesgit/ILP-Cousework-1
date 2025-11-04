package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
@Getter
@Setter
public class MedDispatchRec {
    private int id;
    private LocalDate date;
    private LocalTime time;
    private Requirement requirements;
}
