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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DroneDetailsTests {

    // Mock of the ILP client dependency used inside the service
    @Mock
    private IlpClientComponent ilpClient;

    // Real service under test, with the mock injected
    @InjectMocks
    private DroneServiceImpl droneService;

    // Helper to construct a minimal Drone object with the given ID
    private Drone drone(String id) {
        Drone d = new Drone();
        d.setId(id);
        return d;
    }

    @Test
    void existingId_returnsMatchingDrone() {
        // Arrange: ILP client returns a list that contains the target ID
        List<Drone> allDrones = Arrays.asList(
                drone("D1"),
                drone("D2")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        // Act
        Drone result = droneService.droneDetails("D1");

        // Assert
        assertNotNull(result);
        assertEquals("D1", result.getId());
    }

    @Test
    void nonExistingId_returnsNull() {
        // Arrange: list contains other IDs, but not the requested one
        List<Drone> allDrones = Arrays.asList(
                drone("D2"),
                drone("D3")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        // Act
        Drone result = droneService.droneDetails("D1");

        // Assert
        assertNull(result);
    }

    @Test
    void emptyList_returnsNull() {
        // Arrange: ILP client returns an empty list
        when(ilpClient.getAllDrones()).thenReturn(Collections.emptyList());

        // Act
        Drone result = droneService.droneDetails("D1");

        // Assert
        assertNull(result);
    }

    @Test
    void nullId_returnsNull() {
        // Arrange: ILP client still returns some drones,
        // but the requested ID is null, so nothing should match
        List<Drone> allDrones = Arrays.asList(
                drone("D1"),
                drone("D2")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        // Act
        Drone result = droneService.droneDetails(null);

        // Assert
        assertNull(result);
    }
}


