package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(OpenSAMLMockitoRunner.class)
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