package uk.ac.ed.acp.cw2.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Integration tests for the /nextPosition endpoint.
// Verify REST contract and input validation; core geometry logic is unit-tested elsewhere.
@SpringBootTest
@AutoConfigureMockMvc
class NextPositionEndpointTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void validReturns200() throws Exception {
        String body = """
        {
          "start": { "lng": -3.192473, "lat": 55.946233 },
          "angle": 90
        }""";

        MvcResult res = mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
    }


    @Test
    void ignoresExtraFieldsReturn200() throws Exception {
        // spec: extra JSON members must be ignored
        String body = """
        {
          "start": { "lng": -3.192473, "lat": 55.946233, "foo": 123 },
          "angle": "90",
          "bar": true
        }""";

        MvcResult res = mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void invalidAngleReturns400() throws Exception {
        // 37° is not a multiple of 22.5°
        String body = """
        {
          "start": { "lng": -3.0, "lat": 55.0 },
          "angle": 37
        }""";

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeAngleReturns400() throws Exception {
        // Negative angle is invalid
        String body = """
        {
          "start": { "lng": 0.0, "lat": 0.0 },
          "angle": -10
        }""";

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingStartReturns400() throws Exception {
        String body = """
        { "angle": 45 }""";

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nullAngleReturns400() throws Exception {
        String body = """
        {
          "start": { "lng": -3.192473, "lat": 55.946233 },
          "angle": null
        }""";

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void typeErrorOnAngleReturns400() throws Exception {
        String body = """
        {
          "start": { "lng": -3.192473, "lat": 55.946233 },
          "angle": "abc"
        }""";

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        String body = "{ \"start\": { \"lng\": -3.1, \"lat\": 55.9 }, \"angle\": 0 "; // missing }

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

