package uk.ac.ed.acp.cw2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    // Tests for endpoint 3: /distanceTo
    //
    // These tests ensure the shared helper functions for coordinate validation are fully
    // exercised. Other endpoints (e.g. /isCloseTo) reuse the same helper, so their tests
    // only check functional differences and do not repeat the same invalid cases.

    // valid input returning 200 and the correct numeric result
    @Test
    void distanceToValidRequestShouldReturn200() throws Exception {
        String body = """
            {
              "position1": { "lng": -3.192473, "lat": 55.946233 },
              "position2": { "lng": -3.192473, "lat": 55.942617 }
            }
            """;

        MvcResult res = mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        Double value = Double.parseDouble(res.getResponse().getContentAsString());
        org.hamcrest.MatcherAssert.assertThat(value,
                org.hamcrest.number.IsCloseTo.closeTo(0.003616, 1e-6));
    }

    // out-of-range coordinates returning 400
    @Test
    void distanceToInvalidRequestShouldReturn400() throws Exception {
        String body = """
            {
              "position1": { "lng": -181, "lat": 55.9 },
              "position2": { "lng": -3.1, "lat": 95 }
            }
            """;

        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // type error returning 400
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

    // null field returning 400
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

    // null coordinate field returning 400
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


    // Tests for endpoint 4: /isCloseTo

    // two close coordinate returning true
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

    // two far coordinate returning false
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
}
