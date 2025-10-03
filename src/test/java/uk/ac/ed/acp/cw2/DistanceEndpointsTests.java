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
class DistanceEndpointsTests {

    @Autowired
    private MockMvc mockMvc;

    // Tests for endpoint 3: /distanceTo
    // These tests fully exercise the shared helper functions for coordinate validation.
    // Other endpoints reuse the same helper, so their tests will not repeat coordinate validation checks.
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
    void isCloseToShouldReturnTrueAtBoardCase() throws Exception {
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
    void  isCloseToShouldReturnFalseAtBoradCase() throws Exception {
        String body = """
            {
              "position1": { "lng": -3.192473, "lat": 56.0 },
              "position2": { "lng": -3.192473, "lat": 56.000151 }
            }
            """;
        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
