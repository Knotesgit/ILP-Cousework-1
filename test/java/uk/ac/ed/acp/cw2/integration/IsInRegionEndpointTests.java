package uk.ac.ed.acp.cw2.integration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Integration tests for the /isInRegion endpoint.
// Verify REST contract ,input validation and core geometry logic.
@SpringBootTest
@AutoConfigureMockMvc
public class IsInRegionEndpointTests
{
    @Autowired
    MockMvc mockMvc;

    // helper JSON for a closed rectangle
    private String closedRegionJson(String posLngLat) {
        return """
        {
          "position": %s,
          "region": {
            "name": "central",
            "vertices": [
              { "lng": -3.192473, "lat": 55.946233 },
              { "lng": -3.192473, "lat": 55.942617 },
              { "lng": -3.184319, "lat": 55.942617 },
              { "lng": -3.184319, "lat": 55.946233 },
              { "lng": -3.192473, "lat": 55.946233 }
            ]
          }
        }
        """.formatted(posLngLat);
    }

    @Test
    void insidePointShouldReturnTrue() throws Exception {
        String body = closedRegionJson("{ \"lng\": -3.19, \"lat\": 55.944 }");
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void outsidePointShouldReturnFalse() throws Exception {
        String body = closedRegionJson("{ \"lng\": -3.20, \"lat\": 55.950 }");
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void boundaryPointShouldReturnTrue() throws Exception {
        String body = closedRegionJson("{ \"lng\": -3.192473, \"lat\": 55.944 }");
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void vertexPointShouldReturnTrue() throws Exception {
        String body = closedRegionJson("{ \"lng\": -3.192473, \"lat\": 55.946233 }");
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void nearlyOnEdgeButOutsideShouldReturnFalse() throws Exception {
        String body = closedRegionJson("{ \"lng\": -3.1924730001, \"lat\": 55.944 }");
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void rayHitsVertexShouldNotDoubleCount() throws Exception {
        String body = closedRegionJson("{ \"lng\": -3.19, \"lat\": 55.942617 }");
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }


    @Test
    void openPolygonShouldReturn400() throws Exception {
        String body = """
        {
          "position": { "lng": -3.19, "lat": 55.944 },
          "region": {
            "name": "central",
            "vertices": [
              { "lng": -3.192473, "lat": 55.946233 },
              { "lng": -3.192473, "lat": 55.942617 },
              { "lng": -3.184319, "lat": 55.942617 },
              { "lng": -3.184319, "lat": 55.946233 }
            ]
          }
        }
        """;
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tooFewVerticesShouldReturn400() throws Exception {
        String body = """
        {
          "position": { "lng": -3.19, "lat": 55.944 },
          "region": {
            "name": "central",
            "vertices": [
              { "lng": -3.192473, "lat": 55.946233 },
              { "lng": -3.192473, "lat": 55.942617 },
              { "lng": -3.184319, "lat": 55.942617 }
            ]
          }
        }
        """;
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

}
