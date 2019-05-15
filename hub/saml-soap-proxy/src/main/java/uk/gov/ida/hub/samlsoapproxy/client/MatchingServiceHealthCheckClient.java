package uk.gov.ida.hub.samlsoapproxy.client;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlsoapproxy.domain.MatchingServiceHealthCheckResponseDto;
import uk.gov.ida.hub.samlsoapproxy.rest.HealthCheckResponse;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.inject.Inject;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Optional;

public class MatchingServiceHealthCheckClient {

    private static final Logger LOG = LoggerFactory.getLogger(MatchingServiceHealthCheckClient.class);

    private final HealthCheckSoapRequestClient client;
    private final MetricRegistry metricsRegistry;

    @Inject
    public MatchingServiceHealthCheckClient(HealthCheckSoapRequestClient soapRequestClient, MetricRegistry metricsRegistry) {
        this.client = soapRequestClient;
        this.metricsRegistry = metricsRegistry;
    }

    public MatchingServiceHealthCheckResponseDto sendHealthCheckRequest(
            final Element matchingServiceHealthCheckRequest,
            final URI matchingServiceUri) {

        // Use a custom timer so that we get separate metrics for each matching service
        final String scope = matchingServiceUri.toString().replace(':','_').replace('/', '_');
        final Timer timer = metricsRegistry.timer(MetricRegistry.name(MatchingServiceHealthCheckClient.class, "sendHealthCheckRequest", scope));
        final Timer.Context context = timer.time();
        HealthCheckResponse healthCheckResponse;
        try {
            healthCheckResponse = client.makeSoapRequestForHealthCheck(matchingServiceHealthCheckRequest, matchingServiceUri);
        } catch(ApplicationException ex) {
            final String errorMessage = MessageFormat.format("Failed to complete matching service health check to {0}.", matchingServiceUri);
            LOG.warn(errorMessage, ex);
            return new MatchingServiceHealthCheckResponseDto(Optional.<String>empty());
        } finally {
            context.stop();
        }

        return new MatchingServiceHealthCheckResponseDto(
                    Optional.of(XmlUtils.writeToString(healthCheckResponse.getResponseElement())));
    }
}
