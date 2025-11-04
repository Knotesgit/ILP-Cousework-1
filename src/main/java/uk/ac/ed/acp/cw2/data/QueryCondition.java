package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;


// Represents a single query condition for the POST /api/v1/query endpoint.
// Each condition consists of an attribute name, a comparison operator, and a value.
@Getter
@Setter
public class QueryCondition {
    private String attribute;
    private String operator;
    private String value;
}
