package uk.gov.ida.hub.samlsoapproxy.service;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthCheckDetails;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_INFO;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_INFO_HELP;

public class MatchingServiceInfoMetric extends Collector {
    private final ConcurrentMap<String, MatchingServiceHealthCheckDetails> data;

    public MatchingServiceInfoMetric() {
        this.data = new ConcurrentHashMap<>();
    }

    public void recordDetails(MatchingServiceHealthCheckDetails details) {
        data.put(details.getMatchingService().toString(), details);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return data.values().stream().map(this::infoMetricFor).collect(Collectors.toList());
    }

    private MetricFamilySamples infoMetricFor(MatchingServiceHealthCheckDetails details) {
        GaugeMetricFamily info = new GaugeMetricFamily(VERIFY_SAML_SOAP_PROXY_MSA_INFO, VERIFY_SAML_SOAP_PROXY_MSA_INFO_HELP, Arrays.asList("matchingService", "versionNumber", "versionSupported", "eidasEnabled", "shouldSignWithSha1", "onboarding"));
        info.addMetric(Arrays.asList(details.getMatchingService().toString(),
                details.getVersionNumber(),
                String.valueOf(details.isVersionSupported()),
                details.getEidasEnabled(),
                details.getShouldSignWithSha1(),
                String.valueOf(details.isOnboarding())),
                1.0
                );
        return info;
    }
}
