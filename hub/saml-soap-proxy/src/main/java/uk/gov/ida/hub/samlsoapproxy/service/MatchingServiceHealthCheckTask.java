package uk.gov.ida.hub.samlsoapproxy.service;

import io.prometheus.client.Gauge;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthCheckResult;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthChecker;

import java.util.concurrent.Callable;

public class MatchingServiceHealthCheckTask implements Callable<String> {
    private static final Logger LOG = LoggerFactory.getLogger(MatchingServiceHealthCheckTask.class);
    private final MatchingServiceHealthChecker matchingServiceHealthChecker;
    private final MatchingServiceConfigEntityDataDto matchingServiceConfig;
    private final Gauge healthStatusGauge;
    private final Gauge healthStatusLastUpdatedGauge;
    private final Gauge informationGauge;
    private final Gauge informationLastUpdatedGauge;
    public static final double HEALTHY = 1.0;
    public static final double UNHEALTHY = 0.0;

    public MatchingServiceHealthCheckTask(final MatchingServiceHealthChecker matchingServiceHealthChecker,
                                          final MatchingServiceConfigEntityDataDto matchingServiceConfig,
                                          final Gauge healthStatusGauge,
                                          final Gauge healthStatusLastUpdatedGauge,
                                          final Gauge informationGauge,
                                          final Gauge informationLastUpdatedGauge) {
        this.matchingServiceHealthChecker = matchingServiceHealthChecker;
        this.matchingServiceConfig = matchingServiceConfig;
        this.healthStatusGauge = healthStatusGauge;
        this.healthStatusLastUpdatedGauge = healthStatusLastUpdatedGauge;
        this.informationGauge = informationGauge;
        this.informationLastUpdatedGauge = informationLastUpdatedGauge;
    }

    @Override
    public String call() {
        try {
            final MatchingServiceHealthCheckResult matchingServiceHealthCheckResult = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfig);
            final double timestamp = DateTime.now(DateTimeZone.UTC).getMillis();
            if (matchingServiceHealthCheckResult.isHealthy()) {
                healthStatusGauge.labels(matchingServiceHealthCheckResult.getDetails().getMatchingService().toString())
                                 .set(HEALTHY);
                healthStatusLastUpdatedGauge.labels(matchingServiceHealthCheckResult.getDetails().getMatchingService().toString())
                                            .set(timestamp);
                addAnInformationGauge(informationGauge, matchingServiceHealthCheckResult, HEALTHY);
                addAnInformationGauge(informationLastUpdatedGauge, matchingServiceHealthCheckResult, timestamp);
            } else {
                healthStatusGauge.labels(matchingServiceHealthCheckResult.getDetails().getMatchingService().toString())
                                 .set(UNHEALTHY);
                healthStatusLastUpdatedGauge.labels(matchingServiceHealthCheckResult.getDetails().getMatchingService().toString())
                                            .set(timestamp);
            }
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        return DateTime.now(DateTimeZone.UTC) + matchingServiceConfig.getEntityId();
    }

    private void addAnInformationGauge(final Gauge gauge,
                                       final MatchingServiceHealthCheckResult matchingServiceHealthCheckResult,
                                       final double value) {
        gauge.labels(
            matchingServiceHealthCheckResult.getDetails().getMatchingService().toString(),
            matchingServiceHealthCheckResult.getDetails().getVersionNumber(),
            String.valueOf(matchingServiceHealthCheckResult.getDetails().isVersionSupported()),
            matchingServiceHealthCheckResult.getDetails().getEidasEnabled(),
            matchingServiceHealthCheckResult.getDetails().getShouldSignWithSha1(),
            String.valueOf(matchingServiceHealthCheckResult.getDetails().isOnboarding()))
             .set(value);
    }
}
