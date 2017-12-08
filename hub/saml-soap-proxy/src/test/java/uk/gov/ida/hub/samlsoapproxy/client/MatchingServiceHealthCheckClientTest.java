package uk.gov.ida.hub.samlsoapproxy.client;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Element;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlsoapproxy.domain.MatchingServiceHealthCheckResponseDto;
import uk.gov.ida.hub.samlsoapproxy.rest.HealthCheckResponse;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.samlsoapproxy.builders.HealthCheckResponseBuilder.aHealthCheckResponse;

@RunWith(MockitoJUnitRunner.class)
public class MatchingServiceHealthCheckClientTest {
    @Mock
    HealthCheckSoapRequestClient soapRequestClient;
    @Mock
    Element healthCheckRequest;
    @Mock
    Element healthCheckResponseElement;
    @Mock(answer = Answers.RETURNS_MOCKS)
    MetricRegistry metricsRegistry;

    private final URI healthCheckUri = URI.create("http://some-uri");

    private MatchingServiceHealthCheckClient matchingServiceHealthCheckClient;

    @Before
    public void setup() {
        matchingServiceHealthCheckClient = new MatchingServiceHealthCheckClient(soapRequestClient, metricsRegistry);
    }

    @Test
    public void sendHealthCheckRequest_shouldReturnTrueIfResponseReceived() {
        HealthCheckResponse healthCheckResponse = aHealthCheckResponse()
                .withElement(healthCheckResponseElement)
                .build();

        when(soapRequestClient.makeSoapRequestForHealthCheck(healthCheckRequest, healthCheckUri)).thenReturn(healthCheckResponse);

        final MatchingServiceHealthCheckResponseDto matchingServiceHealthCheckResponseDto =
                matchingServiceHealthCheckClient.sendHealthCheckRequest(healthCheckRequest, healthCheckUri);

        assertThat(matchingServiceHealthCheckResponseDto.getResponse()).isEqualTo(Optional.of(XmlUtils.writeToString(healthCheckResponseElement)));
    }

    @Test
    public void sendHealthCheckRequest_shouldReturnSuccessResponseWithVersionNumber() {
        String expectedVersion = "someVersion";
        HealthCheckResponse responseMessageThatHasAVersion = aHealthCheckResponse()
                .withElement(healthCheckResponseElement)
                .withVersionNumber(expectedVersion)
                .build();

        when(soapRequestClient.makeSoapRequestForHealthCheck(healthCheckRequest, healthCheckUri)).thenReturn(responseMessageThatHasAVersion);

        final MatchingServiceHealthCheckResponseDto matchingServiceHealthCheckResponseDto =
                matchingServiceHealthCheckClient.sendHealthCheckRequest(healthCheckRequest, healthCheckUri);

        assertThat(matchingServiceHealthCheckResponseDto.getVersionNumber()).isEqualTo(Optional.of(expectedVersion));
    }

    @Test
    public void sendHealthCheckRequest_shouldReturnOptionalAbsentIfNoResponseReceived() {
        when(soapRequestClient.makeSoapRequestForHealthCheck(healthCheckRequest, healthCheckUri)).thenThrow(ApplicationException.createUnauditedException(ExceptionType.NETWORK_ERROR, UUID.randomUUID()));

        final MatchingServiceHealthCheckResponseDto matchingServiceHealthCheckResponseDto =
                matchingServiceHealthCheckClient.sendHealthCheckRequest(healthCheckRequest, healthCheckUri);

        assertThat(matchingServiceHealthCheckResponseDto.getResponse()).isEqualTo(Optional.<String>absent());
    }
}
