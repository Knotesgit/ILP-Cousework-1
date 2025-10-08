package uk.ac.ed.acp.cw2.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Integration tests for the /distanceTo and /isCloseTo endpoints.
// Verify REST contract and input validation; distance and threshold logic are unit-tested elsewhere.
@SpringBootTest
@AutoConfigureMockMvc
class DistanceEndpointsTests {

    @Autowired
    private MockMvc mockMvc;
    @Test
    void distanceTo_validRequest_returns200_andJsonNumber() throws Exception {
        String body = """
          { "position1": { "lng": -3.192473, "lat": 55.946233 },
            "position2": { "lng": -3.192473, "lat": 55.942617 } }
        """;

        var res = mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        // Assert only that the response can be parsed as a double;
        // numerical accuracy is covered by unit tests.
        assertDoesNotThrow(() -> Double.parseDouble(res.getResponse().getContentAsString()));
    }

    @Test
    void distanceTo_typeError_should400() throws Exception {
        String body = """
          {
            "position1": { "lng": "abc", "lat": 55.946233 },
            "position2": { "lng": -3.192473, "lat": 55.942617 }
          }
          """;

        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void distanceTo_nullSecondPosition_should400() throws Exception {
        String body = """
          {
            "position1": { "lng": -3.192473, "lat": 55.946233 },
            "position2": null
          }
          """;
        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void distanceTo_nullCoordinateField_should400() throws Exception {
        String body = """
          {
            "position1": { "lng": -3.192473, "lat": 55.946233 },
            "position2": { "lng": null, "lat": 51.5 }
          }
          """;
        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void distanceTo_missingPosition1_returns400() throws Exception {
        String body = """
          { "position2": { "lng": -3.19, "lat": 55.94 } }
        """;
        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void distanceTo_missingLatInsidePosition_returns400() throws Exception {
        String body = """
          {
            "position1": { "lng": -3.19, "lat": 55.94 },
            "position2": { "lng": -3.19 }
          }
        """;
        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // Tests for endpoint 4: /isCloseTo
    @Test
    void isCloseToShouldReturnTrueWhenClose() throws Exception {
        String body = """
            {
              "position1": { "lng": -3.192473, "lat": 55.946233 },
              "position2": { "lng": -3.192473, "lat": 55.946300 }
            }
            """;

        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void isCloseToShouldReturnFalseWhenFar() throws Exception {
        String body = """
            {
              "position1": { "lng": -3.192473, "lat": 55.946233 },
              "position2": { "lng": -3.192473, "lat": 56.0 }
            }
            """;

        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void isCloseToShouldReturnTrueAtBoundaryCase() throws Exception {
        String body = """
            {
              "position1": { "lng": -3.192473, "lat": 55.0 },
              "position2": { "lng": -3.192473, "lat": 55.000149 }
            }
            """;

        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void  isCloseToShouldReturnFalseAtBoundaryCase() throws Exception {
        String body = """
            {
              "position1": { "lng": -3.192473, "lat": 56.0 },
              "position2": { "lng": -3.192473, "lat": 56.00015 }
            }
            """;
        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
