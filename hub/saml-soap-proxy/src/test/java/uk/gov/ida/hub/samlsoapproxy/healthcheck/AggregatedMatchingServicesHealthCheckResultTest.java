package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(OpenSAMLExtension.class)
@ExtendWith(MockitoExtension.class)
public class AggregatedMatchingServicesHealthCheckResultTest {

    private AggregatedMatchingServicesHealthCheckResult result;

    @Mock
    private MatchingServiceHealthCheckDetails msaDetails;

    @Test
    public void isHealthyShouldBeTrueWhenAtLeastOneMSAIsUp() {
        result = new AggregatedMatchingServicesHealthCheckResult();

        when(msaDetails.isOnboarding()).thenReturn(false);

        result.addResult(MatchingServiceHealthCheckResult.healthy(msaDetails));
        result.addResult(MatchingServiceHealthCheckResult.unhealthy(msaDetails));

        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    public void isHealthyShouldBeFalseWhenNoMSAIsUp() {
        result = new AggregatedMatchingServicesHealthCheckResult();

        when(msaDetails.isOnboarding()).thenReturn(false);

        result.addResult(MatchingServiceHealthCheckResult.unhealthy(msaDetails));
        result.addResult(MatchingServiceHealthCheckResult.unhealthy(msaDetails));

        assertThat(result.isHealthy()).isFalse();
    }
}