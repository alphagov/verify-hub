package uk.gov.ida.hub.samlsoapproxy.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.AggregatedMatchingServicesHealthCheckResult;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthCheckHandler;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceHealthCheckDetailsBuilder.aMatchingServiceHealthCheckDetails;
import static uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthCheckResult.unhealthy;

@ExtendWith(MockitoExtension.class)
public class MatchingServiceHealthCheckResourceTest {

    @Mock
    private MatchingServiceHealthCheckHandler handler;

    private MatchingServiceHealthCheckResource matchingServiceHealthCheckResource;

    @BeforeEach
    public void setUp() throws Exception {
        matchingServiceHealthCheckResource = new MatchingServiceHealthCheckResource(handler);
    }

    @Test
    public void performMatchingServiceHealthCheck_shouldReturn500WhenThereAreNoMSAResults() {
        AggregatedMatchingServicesHealthCheckResult result = new AggregatedMatchingServicesHealthCheckResult();
        when(handler.handle()).thenReturn(result);

        final Response response = matchingServiceHealthCheckResource.performMatchingServiceHealthCheck();

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void performMatchingServiceHealthCheck_shouldReturn500WhenHealthCheckIsUnhealthy() {
        AggregatedMatchingServicesHealthCheckResult result = new AggregatedMatchingServicesHealthCheckResult();
        result.addResult(unhealthy(aMatchingServiceHealthCheckDetails().build()));
        when(handler.handle()).thenReturn(result);

        final Response response = matchingServiceHealthCheckResource.performMatchingServiceHealthCheck();

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void performMatchingServiceHealthCheck_shouldReturnResultFromHandler() {
        final String failureDescription = "some-error-description";
        AggregatedMatchingServicesHealthCheckResult result = new AggregatedMatchingServicesHealthCheckResult();
        result.addResult(unhealthy(aMatchingServiceHealthCheckDetails().withDetails(failureDescription).build()));
        when(handler.handle()).thenReturn(result);

        final Response response = matchingServiceHealthCheckResource.performMatchingServiceHealthCheck();

        AggregatedMatchingServicesHealthCheckResult returnedResult = (AggregatedMatchingServicesHealthCheckResult)response.getEntity();
        assertThat(returnedResult).isEqualTo(result);
    }
}
