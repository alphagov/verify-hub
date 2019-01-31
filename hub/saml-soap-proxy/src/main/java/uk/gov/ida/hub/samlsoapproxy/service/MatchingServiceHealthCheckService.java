package uk.gov.ida.hub.samlsoapproxy.service;

import io.dropwizard.util.Duration;
import io.prometheus.client.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthChecker;
import uk.gov.ida.hub.samlsoapproxy.proxy.MatchingServiceConfigProxy;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MatchingServiceHealthCheckService implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MatchingServiceHealthCheckService.class);
    private final ExecutorService matchingServiceHealthCheckTaskManager;
    private final Duration timeout;
    private final MatchingServiceConfigProxy matchingServiceConfigProxy;
    private final MatchingServiceHealthChecker matchingServiceHealthChecker;
    private final Gauge healthStatusGauge;
    private final Gauge healthStatusLastUpdatedGauge;
    private final Gauge informationGauge;
    private final Gauge informationLastUpdatedGauge;

    public MatchingServiceHealthCheckService(final ExecutorService matchingServiceHealthCheckTaskManager,
                                             final Duration timeout,
                                             final MatchingServiceConfigProxy matchingServiceConfigProxy,
                                             final MatchingServiceHealthChecker matchingServiceHealthChecker,
                                             final Gauge healthStatusGauge,
                                             final Gauge healthStatusLastUpdatedGauge,
                                             final Gauge informationGauge,
                                             final Gauge informationLastUpdatedGauge) {
        this.matchingServiceHealthCheckTaskManager = matchingServiceHealthCheckTaskManager;
        this.timeout = timeout;
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
        this.matchingServiceHealthChecker = matchingServiceHealthChecker;
        this.healthStatusGauge = healthStatusGauge;
        this.healthStatusLastUpdatedGauge = healthStatusLastUpdatedGauge;
        this.informationGauge = informationGauge;
        this.informationLastUpdatedGauge = informationLastUpdatedGauge;
    }

    @Override
    public void run() {
        try {
            Collection<MatchingServiceConfigEntityDataDto> matchingServicesToHealthCheck = getUniqueHealthCheckEnabledMatchingServiceConfigs();
            List<Callable<String>> callables =
                matchingServicesToHealthCheck.stream()
                                             .map(matchingServiceInformation ->
                                                      new MatchingServiceHealthCheckTask(
                                                          matchingServiceHealthChecker,
                                                          matchingServiceInformation,
                                                          healthStatusGauge,
                                                          healthStatusLastUpdatedGauge,
                                                          informationGauge,
                                                          informationLastUpdatedGauge))
                                             .collect(Collectors.toList());
            matchingServiceHealthCheckTaskManager.invokeAll(callables, timeout.toSeconds() + 1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    private Collection<MatchingServiceConfigEntityDataDto> getUniqueHealthCheckEnabledMatchingServiceConfigs() {
        final Collection<MatchingServiceConfigEntityDataDto> allMatchingServices = matchingServiceConfigProxy.getMatchingServices();

        // remove duplicated MSAs from the list; we ignore transactionEntityIds when reporting results, so just take the
        // first instance of each MSA
        Set<String> uniqueMatchingServiceEntityIds = new HashSet<>();
        final List<MatchingServiceConfigEntityDataDto> uniqueMatchingServices =
            allMatchingServices.stream()
                               .filter(dto -> uniqueMatchingServiceEntityIds.add(dto.getEntityId()))
                               .collect(Collectors.toList());

        return getMatchingServicesWithEnabledHealthCheck(uniqueMatchingServices);
    }

    private Collection<MatchingServiceConfigEntityDataDto> getMatchingServicesWithEnabledHealthCheck(
        final Collection<MatchingServiceConfigEntityDataDto> matchingServices) {
        return matchingServices.stream()
                               .filter(MatchingServiceConfigEntityDataDto::isHealthCheckEnabled)
                               .collect(Collectors.toList());
    }
}
