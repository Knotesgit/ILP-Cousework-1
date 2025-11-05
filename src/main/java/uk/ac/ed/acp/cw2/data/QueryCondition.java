package uk.ac.ed.acp.cw2.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


// Represents a single query condition for the POST /api/v1/query endpoint.
// Each condition consists of an attribute name, a comparison operator, and a value.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryCondition {
    private String attribute;
    private String operator;
    private String value;
}
