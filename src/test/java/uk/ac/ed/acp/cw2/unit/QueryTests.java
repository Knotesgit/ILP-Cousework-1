package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ed.acp.cw2.data.Drone;
import uk.ac.ed.acp.cw2.data.QueryCondition;
import uk.ac.ed.acp.cw2.external.IlpClientComponent;
import uk.ac.ed.acp.cw2.external.DroneServiceImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class QueryTests {

    @Mock
    private IlpClientComponent ilpClient;

    @InjectMocks
    private DroneServiceImpl droneService;

    private Drone drone(String id) {
        Drone d = new Drone();
        d.setId(id);
        d.setName("Test-" + id);
        Drone.DroneCapability cap = new Drone.DroneCapability();
        d.setCapability(cap);
        return d;
    }

    private QueryCondition cond(String attr, String op, String value) {
        QueryCondition c = new QueryCondition();
        c.setAttribute(attr);
        c.setOperator(op);
        c.setValue(value);
        return c;
    }

    @Test
    void singleMatch_returnsSingleId() {
        // Arrange: three drones, only one satisfies the condition id = "D2"
        List<Drone> allDrones = Arrays.asList(
                drone("D1"),
                drone("D2"),
                drone("D3")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        List<QueryCondition> conditions = List.of(
                cond("id", "=", "D2")
        );

        // Act
        List<String> result = droneService.query(conditions);

        // Assert
        assertEquals(1, result.size());
        assertEquals("D2", result.get(0));
        verify(ilpClient, times(1)).getAllDrones();
    }

    @Test
    void multipleMatches_preserveOrder() {
        // Arrange: two drones with id D1, one with id D2
        List<Drone> allDrones = Arrays.asList(
                drone("D1"),
                drone("D2"),
                drone("D1")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        List<QueryCondition> conditions = List.of(
                cond("id", "=", "D1")
        );

        // Act
        List<String> result = droneService.query(conditions);

        // Assert: only D1 entries, in original order
        assertEquals(Arrays.asList("D1", "D1"), result);
        verify(ilpClient, times(1)).getAllDrones();
    }

    @Test
    void noMatches_returnsEmptyList() {
        // Arrange: no drone satisfies id = "Z"
        List<Drone> allDrones = Arrays.asList(
                drone("A"),
                drone("B")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        List<QueryCondition> conditions = List.of(
                cond("id", "=", "Z")
        );

        // Act
        List<String> result = droneService.query(conditions);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ilpClient, times(1)).getAllDrones();
    }

    @Test
    void emptyDroneList_returnsEmptyList() {
        // Arrange: ILP client returns no drones at all
        when(ilpClient.getAllDrones()).thenReturn(Collections.emptyList());

        List<QueryCondition> conditions = List.of(
                cond("id", "=", "D1")
        );

        // Act
        List<String> result = droneService.query(conditions);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ilpClient, times(1)).getAllDrones();
    }

    @Test
    void emptyConditions_returnsAllIds() {
        // Arrange: matchesConditions(d, emptyList) should return true for all drones
        List<Drone> allDrones = Arrays.asList(
                drone("D1"),
                drone("D2")
        );
        when(ilpClient.getAllDrones()).thenReturn(allDrones);

        List<QueryCondition> conditions = List.of(); // no filters

        // Act
        List<String> result = droneService.query(conditions);

        // Assert: all drone IDs returned in original order
        assertEquals(Arrays.asList("D1", "D2"), result);
        verify(ilpClient, times(1)).getAllDrones();
    }
}

