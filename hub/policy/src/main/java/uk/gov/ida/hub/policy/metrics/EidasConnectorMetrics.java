package uk.gov.ida.hub.policy.metrics;

import io.prometheus.client.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Objects;

public class EidasConnectorMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(EidasConnectorMetrics.class);

    public enum Direction {request, response}

    public enum Status {ok, ko, error}

    private static final Counter CONNECTOR_COUNTER = Counter.build(
            "verify_eidas_connector_count",
            "EIDAS Connector Journey counter with labels 'country', 'direction', 'status'")
            .labelNames("country", "direction", "status")
            .register();

    public static void increment(String entityId, Direction direction, Status status) {
        try {
            String country = getCountryCode(Objects.requireNonNull(entityId));
            CONNECTOR_COUNTER.labels(
                    country.toLowerCase(),
                    Objects.requireNonNull(direction.name()),
                    Objects.requireNonNull(status.name()))
                    .inc();
        } catch (IllegalArgumentException | NullPointerException e) {
            LOG.warn("Could not set counter for entityId '{}', direction '{}', status '{}'",
                    entityId,
                    direction,
                    status,
                    e);
        }
    }

    private static String getCountryCode(String entityId) {
        URI uri = URI.create(entityId.trim());
        String country = uri.getHost();
        String[] hostnameSegments = country.split("\\.");
        return hostnameSegments.length == 0 ? country : hostnameSegments[hostnameSegments.length - 1];
    }
}
