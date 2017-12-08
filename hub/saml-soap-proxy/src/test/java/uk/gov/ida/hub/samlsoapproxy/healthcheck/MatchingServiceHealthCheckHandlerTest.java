package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceConfigEntityDataDtoBuilder;
import uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceHealthCheckDetailsBuilder;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.samlsoapproxy.proxy.MatchingServiceConfigProxy;

import java.net.URI;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;

@RunWith(MockitoJUnitRunner.class)
public class MatchingServiceHealthCheckHandlerTest {
    @Mock
    private MatchingServiceHealthChecker matchingServiceHealthChecker;
    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;

    private MatchingServiceHealthCheckHandler matchingServiceHealthCheckHandler;

    @Before
    public void setUp() throws Exception {
        matchingServiceHealthCheckHandler = new MatchingServiceHealthCheckHandler(
                matchingServiceConfigProxy,
                matchingServiceHealthChecker);
    }

    @Test
    public void handle_shouldReturnSuccessWhenMatchingServiceIsHealthy() throws Exception {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto().build();
        when(matchingServiceConfigProxy.getMatchingServices()).thenReturn(asList(matchingServiceConfigEntityDataDto));
        when(matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto))
                .thenReturn(MatchingServiceHealthCheckResult.healthy(MatchingServiceHealthCheckDetailsBuilder.aMatchingServiceHealthCheckDetails().build()));

        AggregatedMatchingServicesHealthCheckResult result = matchingServiceHealthCheckHandler.handle();

        assertThat(result.isHealthy()).isEqualTo(true);
    }

    @Test
    public void handle_shouldReturnSuccessWhenMatchingServiceIsNotHealthy() throws Exception {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto().build();
        when(matchingServiceConfigProxy.getMatchingServices()).thenReturn(asList(matchingServiceConfigEntityDataDto));
        when(matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto))
                .thenReturn(MatchingServiceHealthCheckResult.unhealthy(MatchingServiceHealthCheckDetailsBuilder.aMatchingServiceHealthCheckDetails().build()));

        AggregatedMatchingServicesHealthCheckResult result = matchingServiceHealthCheckHandler.handle();

        assertThat(result.isHealthy()).isEqualTo(false);
    }

    @Test
    public void handle_shouldCheckHealthOfAllMatchingServices() throws Exception {
        MatchingServiceConfigEntityDataDto firstMatchingService = aMatchingServiceConfigEntityDataDto().withUri(URI.create("/a-matching-service-uri-1")).withEntityId("1").build();
        MatchingServiceConfigEntityDataDto secondMatchingService = aMatchingServiceConfigEntityDataDto().withUri(URI.create("/a-matching-service-uri-2")).withEntityId("2").build();
        Collection<MatchingServiceConfigEntityDataDto> matchingServiceConfigEntityDatas = asList(firstMatchingService, secondMatchingService);
        when(matchingServiceConfigProxy.getMatchingServices()).thenReturn(matchingServiceConfigEntityDatas);
        when(matchingServiceHealthChecker.performHealthCheck(any(MatchingServiceConfigEntityDataDto.class)))
                .thenReturn(MatchingServiceHealthCheckResult.healthy(MatchingServiceHealthCheckDetailsBuilder.aMatchingServiceHealthCheckDetails().build()));

        AggregatedMatchingServicesHealthCheckResult result = matchingServiceHealthCheckHandler.handle();

        verify(matchingServiceHealthChecker).performHealthCheck(firstMatchingService);
        verify(matchingServiceHealthChecker).performHealthCheck(secondMatchingService);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    @Test
    public void handle_shouldNotExecuteHealthCheckForMatchingServiceWithHealthCheckDisabled() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = aMatchingServiceConfigEntityDataDto().withHealthCheckDisabled().build();
        when(matchingServiceConfigProxy.getMatchingServices()).thenReturn(asList(matchingServiceConfigEntityDataDto));

        matchingServiceHealthCheckHandler.handle();

        verify(matchingServiceHealthChecker, never())
                .performHealthCheck(any(MatchingServiceConfigEntityDataDto.class));
    }

    @Test
    public void handle_shouldExecuteHealthCheckForMatchingServiceWithHealthCheckDisabledWhenForced() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = aMatchingServiceConfigEntityDataDto().withHealthCheckDisabled().build();
        when(matchingServiceConfigProxy.getMatchingServices()).thenReturn(asList(matchingServiceConfigEntityDataDto));

        matchingServiceHealthCheckHandler.forceCheckAllMSAs();

        verify(matchingServiceHealthChecker, times(1))
                .performHealthCheck(any(MatchingServiceConfigEntityDataDto.class));
    }

    @Test
    public void handle_shouldExecuteHealthCheckForMatchingServiceWithHealthCheckDisabledWhenForcedIfOneEnabledAndOneDisabled() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDtoHCDisabled = aMatchingServiceConfigEntityDataDto().withHealthCheckDisabled().withEntityId("1").build();
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDtoHCEnabled = aMatchingServiceConfigEntityDataDto().withHealthCheckEnabled().withEntityId("2").build();
        when(matchingServiceConfigProxy.getMatchingServices()).thenReturn(asList(matchingServiceConfigEntityDataDtoHCDisabled, matchingServiceConfigEntityDataDtoHCEnabled));

        matchingServiceHealthCheckHandler.forceCheckAllMSAs();

        verify(matchingServiceHealthChecker, times(2))
                .performHealthCheck(any(MatchingServiceConfigEntityDataDto.class));
    }

    @Test
    public void handle_shouldRemoveDuplicatesFromMatchingServiceList() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = aMatchingServiceConfigEntityDataDto().withHealthCheckDisabled().withTransactionEntityId("1").build();
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto2 = aMatchingServiceConfigEntityDataDto().withHealthCheckDisabled().withTransactionEntityId("2").build();
        when(matchingServiceConfigProxy.getMatchingServices()).thenReturn(asList(matchingServiceConfigEntityDataDto,matchingServiceConfigEntityDataDto2));

        matchingServiceHealthCheckHandler.forceCheckAllMSAs();

        verify(matchingServiceHealthChecker, times(1))
                .performHealthCheck(any(MatchingServiceConfigEntityDataDto.class));
    }

}
