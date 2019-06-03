package uk.gov.ida.hub.config.data;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.ConnectedService;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteCertificateConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConnectedServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteServiceProviderConfig;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

@RunWith(MockitoJUnitRunner.class)
public class ManagedEntityConfigRepositoryTest {

    private static final String REMOTE_ONLY_ENTITY_ID = "https://bananaregistry.test.com";
    private static final String REMOTE_ENABLED_ENTITY_ID = "https://appleregistry.test.com";
    private static final String REMOTE_DISABLED_ENTITY_ID = "https://cherryregistry.test.com";
    private static final String LOCAL_ONLY_ENTITY_ID = "https://local.test.com";
    private static final String BAD_ENTITY_ID = "http://none.existent.test.com";

    private static final String REMOTE_CERT = "MIIDQTCCAiigAwIBAgIBADANBgkqhkiG9w0BAQ0FADA6MQswCQYDVQQGEwJ1azEPMA0GA1UECAwGTG9uZG9uMQwwCgYDVQQKDANHRFMxDDAKBgNVBAMMA2dkczAeFw0xOTA1MTYwOTM4MjlaFw0xOTEwMjgwOTM4MjlaMDoxCzAJBgNVBAYTAnVrMQ8wDQYDVQQIDAZMb25kb24xDDAKBgNVBAoMA0dEUzEMMAoGA1UEAwwDZ2RzMIIBIzANBgkqhkiG9w0BAQEFAAOCARAAMIIBCwKCAQIAxE0gWYnXAqnQf11iWkRDDO+C9C8T+WHrpwxfTtfNILwyHnOhwZNGnO6jjGgQknfiPRVcYcLxkHS54hLlyJjqJA1EPvr/7Zb9VMibsI5wEjglq7E/iZLzsrsqAZ+98fmtodTQPk90sUbOpVi+9eK+oSylqbd4scXyWZ55xSj44xqvqVsOVLLkAFdpgTGrBd6fKx7O+i9tBS5gQVDdFytqOTrD7VrO+pofZX4LWHMoyfksPtpLdASYVnYbO4NG1dxNLq9jmFBMZXR1d8K0i0fF7D7d8mjPDFcOJZSpeLguAXoPKkLfeS6/yr/gex8jDJFtww75LOFIThQCmZMn22YQgOcCAwEAAaNQME4wHQYDVR0OBBYEFOrAl8SufPZEp51+JUisbDJpaFfHMB8GA1UdIwQYMBaAFOrAl8SufPZEp51+JUisbDJpaFfHMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQENBQADggECADRaXNjOl64eqBOMxnkjD0sJwFFIAAzLqxucXcj6SikU+aS7J27fBjjqitw+dHArLry0R6QhgSGpeNEZOu31UoVzS4KL+TDxfQeK/cUgKuqnZQqkeZb3gWmZz4ynnKNUvtzmbA7bXZOgy8jQBGS/lpOprpbsleZywudW8ydn7kuvJMF9G+X7Dlc0S5Gn/PXCDYLS4JAj8uo0RLbKSqMrbnKqSoyugP7C1GPRhLAgbgwn1ozL39nAIlgbKFinuoBGb/B+ZPjpKHvkBt7p7Fngf1zEMR8RyMovKMA/kfYsmPvRxnfT13Qbd5QlNKo4sYj25FTyZfxS1teqfYwwLO4nuLXI";

    @Mock
    private LocalConfigRepository<TransactionConfig> localConfigRepository;

    @Mock
    private S3ConfigSource s3ConfigSource;

    private TransactionConfig localOnlyTransaction = aTransactionConfigData()
            .withEntityId(LOCAL_ONLY_ENTITY_ID)
            .withSelfService(false)
            .build();

    private TransactionConfig remoteEnabledTransaction = aTransactionConfigData()
            .withEntityId(REMOTE_ENABLED_ENTITY_ID)
            .withSelfService(true)
            .build();

    private TransactionConfig remoteDisabledTransaction = aTransactionConfigData()
            .withEntityId(REMOTE_DISABLED_ENTITY_ID)
            .withSelfService(false)
            .build();

    @Before
    public void setUp() throws Exception {
        ConnectedService v1 = new ConnectedService(
            new RemoteConnectedServiceConfig(),
            new RemoteServiceProviderConfig("tbd",
                new RemoteCertificateConfig(
                    "1",
                    "tbd",
                    REMOTE_CERT),
                Collections.emptyList()
            )
        );
        RemoteConfigCollection remoteConfigCollection = new RemoteConfigCollection(null, ImmutableMap.of(REMOTE_ENABLED_ENTITY_ID, v1), ImmutableMap.of());
        when(s3ConfigSource.getRemoteConfig()).thenReturn(remoteConfigCollection);
        when(localConfigRepository.getData(LOCAL_ONLY_ENTITY_ID)).thenReturn(Optional.of(localOnlyTransaction));
        when(localConfigRepository.getData(REMOTE_ENABLED_ENTITY_ID)).thenReturn(Optional.of(remoteEnabledTransaction));
        when(localConfigRepository.getData(REMOTE_DISABLED_ENTITY_ID)).thenReturn(Optional.of(remoteDisabledTransaction));
    }

    @Test
    public void getReturnsOptionalEmptyIfNoLocalConfigFound() {
        ManagedEntityConfigRepository<TransactionConfig> configRepo = new ManagedEntityConfigRepository<>(localConfigRepository, s3ConfigSource);
        Optional<TransactionConfig> result = configRepo.get(BAD_ENTITY_ID);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void getReturnsOptionalEmptyIfNoLocalConfigFoundButRemoteExists() {
        ManagedEntityConfigRepository<TransactionConfig>configRepo = new ManagedEntityConfigRepository<>(localConfigRepository, s3ConfigSource);
        Optional<TransactionConfig> result = configRepo.get(REMOTE_ONLY_ENTITY_ID);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void getReturnsLocalConfigWhenRemoteNotFound() {
        ManagedEntityConfigRepository<TransactionConfig>configRepo = new ManagedEntityConfigRepository<>(localConfigRepository, s3ConfigSource);
        Optional<TransactionConfig> result = configRepo.get(LOCAL_ONLY_ENTITY_ID);

        assertThat(result.get().getEntityId()).isEqualTo(LOCAL_ONLY_ENTITY_ID);
        assertThat(result.get().getEncryptionCertificate().getX509()).isEqualTo(localOnlyTransaction.getEncryptionCertificate().getX509());
    }

    @Test
    public void getReturnsLocalConfigWhenRemoteAvailableButDisabled() {
        ManagedEntityConfigRepository<TransactionConfig>configRepo = new ManagedEntityConfigRepository<>(localConfigRepository, s3ConfigSource);
        Optional<TransactionConfig> result = configRepo.get(REMOTE_DISABLED_ENTITY_ID);

        assertThat(result.get().getEntityId()).isEqualTo(REMOTE_DISABLED_ENTITY_ID);
        assertThat(result.get().getEncryptionCertificate().getX509()).isEqualTo(remoteDisabledTransaction.getEncryptionCertificate().getX509());
    }

    @Test
    public void getReturnsOverriddenConfigWhenRemoteFoundAndEnabled() {
        ManagedEntityConfigRepository<TransactionConfig>configRepo = new ManagedEntityConfigRepository<>(localConfigRepository, s3ConfigSource);
        Optional<TransactionConfig> result = configRepo.get(REMOTE_ENABLED_ENTITY_ID);

        assertThat(result.get().getEntityId()).isEqualTo(REMOTE_ENABLED_ENTITY_ID);
        assertThat(result.get().getEncryptionCertificate().getX509()).isEqualTo(REMOTE_CERT);
    }

    @Test
    public void getAllReturnsConfigAsPerIndividualRules(){

    }

}