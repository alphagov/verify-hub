package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.samlsoapproxy.proxy.MatchingServiceConfigProxy;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MatchingServiceHealthCheckHandler {
    private final MatchingServiceConfigProxy matchingServiceConfigProxy;
    private final MatchingServiceHealthChecker matchingServiceHealthChecker;

    @Inject
    public MatchingServiceHealthCheckHandler(final MatchingServiceConfigProxy matchingServiceConfigProxy,
                                             final MatchingServiceHealthChecker matchingServiceHealthChecker) {
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
        this.matchingServiceHealthChecker = matchingServiceHealthChecker;
    }

    public AggregatedMatchingServicesHealthCheckResult handle() {
        return handle(false);
    }

    public AggregatedMatchingServicesHealthCheckResult forceCheckAllMSAs() {
        return handle(true);
    }

    private AggregatedMatchingServicesHealthCheckResult handle(boolean forceCheckOnAllMSAs) {
        AggregatedMatchingServicesHealthCheckResult aggregatedResult = new AggregatedMatchingServicesHealthCheckResult();

        final Collection<MatchingServiceConfigEntityDataDto> allMatchingServices = matchingServiceConfigProxy.getMatchingServices();

        // remove duplicated MSAs from the list; we ignore transactionEntityIds when reporting results, so just take the
        // first instance of each MSA
        Set<String> uniqueMatchingServiceEntityIds = new HashSet<>();
        final List<MatchingServiceConfigEntityDataDto> uniqueMatchingServices = allMatchingServices
                .stream()
                .filter(dto -> uniqueMatchingServiceEntityIds.add(dto.getEntityId()))
                .collect(Collectors.toList());

        Collection<MatchingServiceConfigEntityDataDto> matchingServicesToHealthCheck;
        if(forceCheckOnAllMSAs) {
            matchingServicesToHealthCheck = uniqueMatchingServices;
        } else {
            matchingServicesToHealthCheck = getMatchingServicesWithEnabledHealthCheck(uniqueMatchingServices);
        }

        for (MatchingServiceConfigEntityDataDto matchingServiceInfo : matchingServicesToHealthCheck) {
            MatchingServiceHealthCheckResult healthCheckResult = matchingServiceHealthChecker.performHealthCheck(matchingServiceInfo);

            aggregatedResult.addResult(healthCheckResult);
        }

        return aggregatedResult;
    }

    private Collection<MatchingServiceConfigEntityDataDto> getMatchingServicesWithEnabledHealthCheck(
            final Collection<MatchingServiceConfigEntityDataDto> matchingServices) {
        return matchingServices.stream()
                               .filter(MatchingServiceConfigEntityDataDto::isHealthCheckEnabled)
                               .collect(Collectors.toList());
    }

}
