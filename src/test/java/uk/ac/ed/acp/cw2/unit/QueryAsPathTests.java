package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ed.acp.cw2.data.Drone;
import uk.ac.ed.acp.cw2.external.IlpClientComponent;
import uk.ac.ed.acp.cw2.external.DroneServiceImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class QueryAsPathTests {

    @Mock
    private IlpClientComponent ilpClient;

    @InjectMocks
    private DroneServiceImpl droneService;

    // Minimal helper to construct a Drone with a given ID
    private Drone drone(String id) {
        Drone d = new Drone();
        d.setId(id);

        // Capability is not used for "id" attribute,
        // but we provide a non-null capability to keep the object well-formed.
        Drone.DroneCapability cap = new Drone.DroneCapability();
        d.setCapability(cap);

        return d;
    }

    @Test
    void singleMatch_returnsSingleId() {
        // Arrange: three drones, only one matches attribute/value
        List<Drone> allDrones = Arrays.asList(
                drone("D1"),
                drone("D2"),
                drone("D3")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        // Act
        List<String> result = droneService.queryAsPath("id", "D2");

        // Assert
        assertEquals(1, result.size());
        assertEquals("D2", result.get(0));
        verify(ilpClient, times(1)).getAllDrones();
    }

    @Test
    void multipleMatches_preserveOrder() {
        // Arrange: two drones with the same id, one different
        List<Drone> allDrones = Arrays.asList(
                drone("X"),
                drone("X"),
                drone("Y")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        // Act
        List<String> result = droneService.queryAsPath("id", "X");

        // Assert: both matching IDs are returned in original order
        assertEquals(Arrays.asList("X", "X"), result);
        verify(ilpClient, times(1)).getAllDrones();
    }

    @Test
    void noMatches_returnsEmptyList() {
        // Arrange: drones exist, but none matches the requested id
        List<Drone> allDrones = Arrays.asList(
                drone("A"),
                drone("B")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        // Act
        List<String> result = droneService.queryAsPath("id", "Z");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ilpClient, times(1)).getAllDrones();
    }

    @Test
    void emptyDroneList_returnsEmptyList() {
        // Arrange: ILP client returns no drones at all
        when(ilpClient.getAllDrones()).thenReturn(Collections.emptyList());

        // Act
        List<String> result = droneService.queryAsPath("id", "D1");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ilpClient, times(1)).getAllDrones();
    }
}
