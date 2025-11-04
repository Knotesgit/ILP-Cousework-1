package uk.ac.ed.acp.cw2.data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Requirement {
    private double capacity;
    private boolean cooling;
    private boolean heating;
    private Double maxCost;
}

