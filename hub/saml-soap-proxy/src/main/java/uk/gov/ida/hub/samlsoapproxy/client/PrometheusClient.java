package uk.gov.ida.hub.samlsoapproxy.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.setup.Environment;
import io.prometheus.client.Gauge;
import uk.gov.ida.hub.samlsoapproxy.SamlSoapProxyConfiguration;
import uk.gov.ida.hub.samlsoapproxy.config.PrometheusClientServiceConfiguration;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthChecker;
import uk.gov.ida.hub.samlsoapproxy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.samlsoapproxy.service.MatchingServiceInfoMetric;
import uk.gov.ida.hub.samlsoapproxy.service.MatchingServiceHealthCheckService;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class PrometheusClient {
    public static final String VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS = "verify_saml_soap_proxy_msa_health_status";
    public static final String VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED = "verify_saml_soap_proxy_msa_health_status_last_updated";
    public static final String VERIFY_SAML_SOAP_PROXY_MSA_INFO = "verify_saml_soap_proxy_msa_info";
    public static final String VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_HELP = "Matching Service Health Status (1 = healthy and 0 = unhealthy)";
    public static final String VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED_HELP = "Matching Service Health Status Metric Last Updated (ms)";
    public static final String VERIFY_SAML_SOAP_PROXY_MSA_INFO_HELP = "Matching Service Information (1 = healthy)";
    private static final String MSA_HEALTH_CHECK_TASK_MANAGER = "MatchingServiceHealthCheckTaskManager";
    private static final boolean USE_DAEMON_THREADS = true;
    private final Environment environment;
    private final SamlSoapProxyConfiguration samlSoapProxyConfiguration;
    private final MatchingServiceConfigProxy matchingServiceConfigProxy;
    private final MatchingServiceHealthChecker matchingServiceHealthChecker;

    @Inject
    public PrometheusClient(final Environment environment,
                            final SamlSoapProxyConfiguration samlSoapProxyConfiguration,
                            final MatchingServiceConfigProxy matchingServiceConfigProxy,
                            final MatchingServiceHealthChecker matchingServiceHealthChecker) {
        this.environment = environment;
        this.samlSoapProxyConfiguration = samlSoapProxyConfiguration;
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
        this.matchingServiceHealthChecker = matchingServiceHealthChecker;
    }

    public void createMatchingServiceHealthCheckMetrics() {
        final PrometheusClientServiceConfiguration configuration = samlSoapProxyConfiguration.getMatchingServiceHealthCheckServiceConfiguration();
        if (configuration.getEnable()) {
            Gauge healthStatusGauge = Gauge.build(VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_HELP)
                                      .labelNames("matchingService")
                                      .register();
            Gauge healthStatusLastUpdatedGauge = Gauge.build(VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED_HELP)
                                                 .labelNames("matchingService")
                                                 .register();
            MatchingServiceInfoMetric infoMetric = new MatchingServiceInfoMetric().register();

            ExecutorService matchingServiceHealthCheckTaskManager =
                environment.lifecycle()
                           .executorService(MSA_HEALTH_CHECK_TASK_MANAGER)
                           .threadFactory(new ThreadFactoryBuilder().setNameFormat(MSA_HEALTH_CHECK_TASK_MANAGER).setDaemon(USE_DAEMON_THREADS).build())
                           .minThreads(configuration.getMinNumOfThreads())
                           .maxThreads(configuration.getMaxNumOfThreads())
                           .keepAliveTime(configuration.getKeepAliveTime())
                           .workQueue(new SynchronousQueue<>())
                           .build();
            MatchingServiceHealthCheckService matchingServiceHealthCheckService = new MatchingServiceHealthCheckService(
                matchingServiceHealthCheckTaskManager,
                samlSoapProxyConfiguration.getHealthCheckSoapHttpClient().getTimeout(),
                matchingServiceConfigProxy,
                matchingServiceHealthChecker,
                healthStatusGauge,
                healthStatusLastUpdatedGauge,
                infoMetric);

            createScheduledExecutorService(configuration, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS, matchingServiceHealthCheckService);
        }
    }

    private void createScheduledExecutorService(final PrometheusClientServiceConfiguration configuration,
                                                final String nameFormat,
                                                final Runnable service) {
        ScheduledExecutorService scheduledExecutorService = environment.lifecycle()
                                                                       .scheduledExecutorService(nameFormat, USE_DAEMON_THREADS)
                                                                       .build();
        scheduledExecutorService.scheduleAtFixedRate(
            service,
            configuration.getInitialDelay().toSeconds(),
            configuration.getDelay().toSeconds(),
            TimeUnit.SECONDS);
    }
}
