package uk.ac.ed.acp.cw2.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.ac.ed.acp.cw2.external.DroneServiceImpl;
import uk.ac.ed.acp.cw2.external.IlpClientComponent;
import uk.ac.ed.acp.cw2.data.Drone;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class DronesWithCoolingTests {

    private IlpClientComponent ilpClient;
    private DroneServiceImpl service;

    @BeforeEach
    void setup() {
        ilpClient = Mockito.mock(IlpClientComponent.class);
        service = new DroneServiceImpl(ilpClient);
    }

    // --- Helper: build drone with id + cooling capability ---
    private Drone drone(String id, boolean cooling) {
        Drone d = new Drone();
        d.setId(id);

        Drone.DroneCapability cap = new Drone.DroneCapability();
        cap.setCooling(cooling);
        d.setCapability(cap);

        return d;
    }


    @Test
    void coolingTrue_filtersCorrectly() {
        when(ilpClient.getAllDrones()).thenReturn(List.of(
                drone("1", true),
                drone("2", false),
                drone("3", true)
        ));

        var result = service.dronesWithCooling(true);

        assertEquals(List.of("1", "3"), result);
    }

    @Test
    void coolingFalse_filtersCorrectly() {
        when(ilpClient.getAllDrones()).thenReturn(List.of(
                drone("1", true),
                drone("2", false),
                drone("3", true)
        ));

        var result = service.dronesWithCooling(false);

        assertEquals(List.of("2"), result);
    }

    @Test
    void emptyInput_returnsEmpty() {
        when(ilpClient.getAllDrones()).thenReturn(List.of());

        assertEquals(List.of(), service.dronesWithCooling(true));
        assertEquals(List.of(), service.dronesWithCooling(false));
    }

    @Test
    void allTrue_or_allFalse() {
        // all true
        when(ilpClient.getAllDrones()).thenReturn(List.of(
                drone("A", true),
                drone("B", true)
        ));
        assertEquals(List.of("A", "B"), service.dronesWithCooling(true));
        assertEquals(List.of(), service.dronesWithCooling(false));

        // all false
        when(ilpClient.getAllDrones()).thenReturn(List.of(
                drone("X", false),
                drone("Y", false)
        ));
        assertEquals(List.of(), service.dronesWithCooling(true));
        assertEquals(List.of("X", "Y"), service.dronesWithCooling(false));
    }
}
