package uk.ac.ed.acp.cw2.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.ac.ed.acp.cw2.data.Coordinate;
import uk.ac.ed.acp.cw2.data.MedDispatchRec;
import uk.ac.ed.acp.cw2.data.response.CalcDeliveryPathResponse;
import uk.ac.ed.acp.cw2.external.DroneServiceImpl;
import uk.ac.ed.acp.cw2.external.IlpClientComponent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DroneServiceImplBackendQueryCountTest {

    @Test
    @DisplayName("FR-I1: calcDeliveryPath queries ILP backend exactly once per request")
    void calcDeliveryPath_queriesBackendExactlyOnce() {
        // Arrange: mock backend client
        IlpClientComponent ilp = Mockito.mock(IlpClientComponent.class);

        // Minimal stubs: return empty lists is fine (we're testing call count, not correctness)
        when(ilp.getAllDrones()).thenReturn(List.of());
        when(ilp.getServicePoints()).thenReturn(List.of());
        when(ilp.getRestrictedAreas()).thenReturn(List.of());
        when(ilp.getDronesForServicePoints()).thenReturn(List.of());

        DroneServiceImpl sut = new DroneServiceImpl(ilp);

        // Minimal valid dispatch record to pass isValidDispatchList(...)
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(1);

        MedDispatchRec.Requirement req = new MedDispatchRec.Requirement();
        req.setCapacity(1.0);
        req.setCooling(false);
        req.setHeating(false);
        rec.setRequirements(req);

        rec.setDelivery(new Coordinate(-3.1883, 55.9533));
        // date/time can both be null (valid), so it becomes "anytime" and planning returns empty response.

        // Act
        CalcDeliveryPathResponse resp = sut.calcDeliveryPath(List.of(rec));

        // Assert: basic sanity (not the focus, but prevents totally broken flow)
        assertNotNull(resp);
        assertNotNull(resp.getDronePaths());

        // Assert: FR-I1 call-count contract
        verify(ilp, times(1)).getAllDrones();
        verify(ilp, times(1)).getServicePoints();
        verify(ilp, times(1)).getRestrictedAreas();
        verify(ilp, times(1)).getDronesForServicePoints();

        // prove no extra ILP calls exist in this request path
        verifyNoMoreInteractions(ilp);
    }
}
