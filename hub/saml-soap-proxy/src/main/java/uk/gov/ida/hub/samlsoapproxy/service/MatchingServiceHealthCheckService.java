package uk.gov.ida.hub.samlsoapproxy.service;

import io.dropwizard.util.Duration;
import io.prometheus.client.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthChecker;
import uk.gov.ida.hub.samlsoapproxy.proxy.MatchingServiceConfigProxy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MatchingServiceHealthCheckService implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MatchingServiceHealthCheckService.class);
    private static final long WAIT_FOR_THREAD_TO_MARK_TIMEOUT_MATCHING_SERVICE_UNHEALTHY = 1L;
    private final ExecutorService matchingServiceHealthCheckTaskManager;
    private final Duration timeout;
    private final MatchingServiceConfigProxy matchingServiceConfigProxy;
    private final MatchingServiceHealthChecker matchingServiceHealthChecker;
    private final Gauge healthStatusGauge;
    private final Gauge healthStatusLastUpdatedGauge;
    private final MatchingServiceInfoMetric infoMetric;

    public MatchingServiceHealthCheckService(final ExecutorService matchingServiceHealthCheckTaskManager,
                                             final Duration timeout,
                                             final MatchingServiceConfigProxy matchingServiceConfigProxy,
                                             final MatchingServiceHealthChecker matchingServiceHealthChecker,
                                             final Gauge healthStatusGauge,
                                             final Gauge healthStatusLastUpdatedGauge,
                                             final MatchingServiceInfoMetric infoMetric) {
        this.matchingServiceHealthCheckTaskManager = matchingServiceHealthCheckTaskManager;
        this.timeout = timeout;
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
        this.matchingServiceHealthChecker = matchingServiceHealthChecker;
        this.healthStatusGauge = healthStatusGauge;
        this.healthStatusLastUpdatedGauge = healthStatusLastUpdatedGauge;
        this.infoMetric = infoMetric;
    }

    @Override
    public void run() {
        try {
            List<Callable<String>> callables =
                    matchingServiceConfigProxy.getMatchingServices().stream()
                        .filter(MatchingServiceConfigEntityDataDto::isHealthCheckEnabled)
                        .map(matchingServiceInformation ->
                                new MatchingServiceHealthCheckTask(
                                        matchingServiceHealthChecker,
                                        matchingServiceInformation,
                                        healthStatusGauge,
                                        healthStatusLastUpdatedGauge,
                                        infoMetric))
                        .collect(Collectors.toList());
            matchingServiceHealthCheckTaskManager.invokeAll(callables, timeout.toSeconds() + WAIT_FOR_THREAD_TO_MARK_TIMEOUT_MATCHING_SERVICE_UNHEALTHY, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error("Failed to perform Matching Health Service Task on all Matching Services.", e);
        }
    }
}
