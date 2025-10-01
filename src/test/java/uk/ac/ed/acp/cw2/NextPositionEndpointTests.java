package uk.ac.ed.acp.cw2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NextPositionEndpointTests {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper om;

    private static final double STEP = 0.00015;
    private static final double EPS  = 1e-12;
    private static final double DIR_STEP = 22.5;

    // helpers
    private static double norm16(double angleDeg) {
        double a = angleDeg % 360.0;
        if (a < 0) a += 360.0;
        double k = Math.round(a / DIR_STEP);
        double rounded = k * DIR_STEP;
        if (rounded >= 360.0) rounded -= 360.0;
        return rounded;
    }
    private static double[] nextFrom(double lng, double lat, double angleDeg) {
        double rad = Math.toRadians(norm16(angleDeg));
        double dx = STEP * Math.cos(rad);
        double dy = STEP * Math.sin(rad);
        return new double[]{ lng + dx, lat + dy };
    }

    @Test
    void nextPosition_valid_returns200_andCorrectCoordinate() throws Exception {
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

        JsonNode json = om.readTree(res.getResponse().getContentAsString());
        double expected[] = nextFrom(-3.192473, 55.946233, 90);

        assertThat(json.get("lng").asDouble(), closeTo(expected[0], 1e-6));
        assertThat(json.get("lat").asDouble(), closeTo(expected[1], 1e-6));
    }

    @Test
    void nextPosition_angleGetsRoundedToNearest22_5() throws Exception {
        // 37° should round to 45°
        String body = """
        {
          "start": { "lng": -3.0, "lat": 55.0 },
          "angle": 37
        }""";

        MvcResult res = mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = om.readTree(res.getResponse().getContentAsString());
        double[] expected = nextFrom(-3.0, 55.0, 45.0);

        assertThat(json.get("lng").asDouble(), closeTo(expected[0], 1e-6));
        assertThat(json.get("lat").asDouble(), closeTo(expected[1], 1e-6));
    }

    @Test
    void nextPosition_negativeAngle_wrapsAndRounds() throws Exception {
        // -10° → wrap to 350°, nearest is 0° (East)
        String body = """
        {
          "start": { "lng": 0.0, "lat": 0.0 },
          "angle": -10
        }""";

        MvcResult res = mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = om.readTree(res.getResponse().getContentAsString());
        double[] expected = nextFrom(0.0, 0.0, 0.0);

        assertThat(json.get("lng").asDouble(), closeTo(expected[0], 1e-6));
        assertThat(json.get("lat").asDouble(), closeTo(expected[1], 1e-6));
    }

    @Test
    void nextPosition_ignoresExtraFields() throws Exception {
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

        JsonNode json = om.readTree(res.getResponse().getContentAsString());
        double[] expected = nextFrom(-3.192473, 55.946233, 90);

        assertThat(json.get("lng").asDouble(), closeTo(expected[0], 1e-6));
        assertThat(json.get("lat").asDouble(), closeTo(expected[1], 1e-6));
    }

    @Test
    void nextPosition_missingStart_returns400() throws Exception {
        String body = """
        { "angle": 45 }""";

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nextPosition_nullAngle_returns400() throws Exception {
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
    void nextPosition_typeErrorOnAngle_returns400() throws Exception {
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
    void nextPosition_invalidCoordinate_returns400() throws Exception {
        String body = """
        {
          "start": { "lng": 181, "lat": 55.946233 },
          "angle": 0
        }""";

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nextPosition_malformedJson_returns400() throws Exception {
        String body = "{ \"start\": { \"lng\": -3.1, \"lat\": 55.9 }, \"angle\": 0 "; // missing }

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

