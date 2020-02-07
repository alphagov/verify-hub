package uk.gov.ida.integrationtest.hub.policy.builders;

import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import io.lettuce.core.RedisURI;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.configuration.RedisConfiguration;
import uk.gov.ida.hub.policy.configuration.SessionStoreConfiguration;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;

import java.net.URI;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ida.common.ServiceInfoConfigurationBuilder.aServiceInfo;

public class PolicyConfigurationBuilder {

    private Duration timeoutPeriod = Duration.minutes(2);
    private ServiceInfoConfiguration serviceInfo = aServiceInfo().withName("Policy").build();
    private int redisPort = RedisURI.DEFAULT_REDIS_PORT;

    public static PolicyConfigurationBuilder aPolicyConfiguration() {
        return new PolicyConfigurationBuilder();
    }

    public PolicyConfiguration build() {
        SessionStoreConfiguration sessionStoreConfiguration = mock(SessionStoreConfiguration.class);
        when(sessionStoreConfiguration.getRedisConfiguration()).thenReturn(Optional.of(
                new RedisConfiguration() {
                    @Override
                    public Long getRecordTTL() {
                        return 3600L;
                    }

                    @Override
                    public RedisURI getUri() {
                        return new RedisURI("localhost",redisPort, java.time.Duration.ofSeconds(1));
                    }
                }
        ));
        return new TestPolicyConfiguration(
                new JerseyClientConfiguration(),
                serviceInfo,
                mock(ClientTrustStoreConfiguration.class),
                sessionStoreConfiguration,
                timeoutPeriod,
                Duration.minutes(1),
                Duration.minutes(15));
    }

    public PolicyConfigurationBuilder withTimeoutPeriod(long i) {
        timeoutPeriod = Duration.minutes(i);
        return this;
    }

    public PolicyConfigurationBuilder withServiceInfo(ServiceInfoConfiguration serviceInfo) {
        this.serviceInfo = serviceInfo;
        return this;
    }

    public PolicyConfigurationBuilder withRedisPort(int redisPort) {
        this.redisPort = redisPort;
        return this;
    }

    private static class TestPolicyConfiguration extends PolicyConfiguration {
        private TestPolicyConfiguration(
                JerseyClientConfiguration httpClient,
                ServiceInfoConfiguration serviceInfo,
                ClientTrustStoreConfiguration clientTrustStoreConfiguration,
                SessionStoreConfiguration sessionStoreConfiguration,
                Duration timeoutPeriod,
                Duration matchingServiceResponseWaitPeriod,
                Duration assertionLifetime) {

            this.eventSinkUri = URI.create("http://event-sink");
            this.samlEngineUri = URI.create("http://saml-engine");
            this.samlSoapProxyUri = URI.create("http://saml-soap-proxy");
            this.httpClient = httpClient;
            this.serviceInfo = serviceInfo;
            this.clientTrustStoreConfiguration = clientTrustStoreConfiguration;
            this.sessionStore = sessionStoreConfiguration;
            this.timeoutPeriod = timeoutPeriod;
            this.matchingServiceResponseWaitPeriod = matchingServiceResponseWaitPeriod;
            this.assertionLifetime = assertionLifetime;
            this.configUri = URI.create("http://config");
        }
    }
}
