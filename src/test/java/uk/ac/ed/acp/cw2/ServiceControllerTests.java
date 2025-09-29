package uk.ac.ed.acp.cw2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    // 3. /distanceTo
    @Test
    void distanceToValidRequestShouldReturn200() throws Exception {
        String body = """
            {
              "position1": { "lng": -3.192473, "lat": 55.946233 },
              "position2": { "lng": -3.192473, "lat": 55.942617 }
            }
            """;

        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.notNullValue()));
    }

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


    // 4. /isCloseTo
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
}
