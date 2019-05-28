package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;

@RunWith(MockitoJUnitRunner.class)
public class MatchingServiceConfigRepositoryTest {
    private static final String REMOTE_ONLY_ENTITY_ID = "https://msa.bananaregistry.test.com";
    private static final String REMOTE_ENABLED_ENTITY_ID = "https://msa.appleregistry.test.com";
    private static final String REMOTE_DISABLED_ENTITY_ID = "https://msa.cherryregistry.test.com";
    private static final String LOCAL_ONLY_ENTITY_ID = "https://msa.local.test.com";
    private static final String BAD_ENTITY_ID = "http://msa.none.existent.test.com";

    private static final String REMOTE_CERT = "MIIDQTCCAiigAwIBAgIBADANBgkqhkiG9w0BAQ0FADA6MQswCQYDVQQGEwJ1azEPMA0GA1UECAwGTG9uZG9uMQwwCgYDVQQKDANHRFMxDDAKBgNVBAMMA2dkczAeFw0xOTA1MTYwOTM4MjlaFw0xOTEwMjgwOTM4MjlaMDoxCzAJBgNVBAYTAnVrMQ8wDQYDVQQIDAZMb25kb24xDDAKBgNVBAoMA0dEUzEMMAoGA1UEAwwDZ2RzMIIBIzANBgkqhkiG9w0BAQEFAAOCARAAMIIBCwKCAQIAxE0gWYnXAqnQf11iWkRDDO+C9C8T+WHrpwxfTtfNILwyHnOhwZNGnO6jjGgQknfiPRVcYcLxkHS54hLlyJjqJA1EPvr/7Zb9VMibsI5wEjglq7E/iZLzsrsqAZ+98fmtodTQPk90sUbOpVi+9eK+oSylqbd4scXyWZ55xSj44xqvqVsOVLLkAFdpgTGrBd6fKx7O+i9tBS5gQVDdFytqOTrD7VrO+pofZX4LWHMoyfksPtpLdASYVnYbO4NG1dxNLq9jmFBMZXR1d8K0i0fF7D7d8mjPDFcOJZSpeLguAXoPKkLfeS6/yr/gex8jDJFtww75LOFIThQCmZMn22YQgOcCAwEAAaNQME4wHQYDVR0OBBYEFOrAl8SufPZEp51+JUisbDJpaFfHMB8GA1UdIwQYMBaAFOrAl8SufPZEp51+JUisbDJpaFfHMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQENBQADggECADRaXNjOl64eqBOMxnkjD0sJwFFIAAzLqxucXcj6SikU+aS7J27fBjjqitw+dHArLry0R6QhgSGpeNEZOu31UoVzS4KL+TDxfQeK/cUgKuqnZQqkeZb3gWmZz4ynnKNUvtzmbA7bXZOgy8jQBGS/lpOprpbsleZywudW8ydn7kuvJMF9G+X7Dlc0S5Gn/PXCDYLS4JAj8uo0RLbKSqMrbnKqSoyugP7C1GPRhLAgbgwn1ozL39nAIlgbKFinuoBGb/B+ZPjpKHvkBt7p7Fngf1zEMR8RyMovKMA/kfYsmPvRxnfT13Qbd5QlNKo4sYj25FTyZfxS1teqfYwwLO4nuLXI";

    @Mock
    private LocalConfigRepository<MatchingServiceConfig> localConfigRepository;

    @Mock
    private S3ConfigSource s3ConfigSource;

    private MatchingServiceConfig localOnlyTransaction = aMatchingServiceConfig()
            .withEntityId(LOCAL_ONLY_ENTITY_ID)
            .withSelfService(false)
            .build();

    private MatchingServiceConfig remoteEnabledTransaction = aMatchingServiceConfig()
            .withEntityId(REMOTE_ENABLED_ENTITY_ID)
            .withSelfService(true)
            .build();

    private MatchingServiceConfig remoteDisabledTransaction = aMatchingServiceConfig()
            .withEntityId(REMOTE_DISABLED_ENTITY_ID)
            .withSelfService(false)
            .build();

    private RemoteConfigCollection remoteConfigCollection;

    @Before
    public void setup() throws Exception {
        URL url = this.getClass().getResource("/remote-test-config.json");
        File initialFile = new File(url.getFile());
        InputStream inputStream = new FileInputStream(initialFile);
        ObjectMapper om = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        om.setDateFormat(df);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(RemoteConfigCollection.class, new RemoteConfigCollectionDeserializer());
        om.registerModule(module);
        remoteConfigCollection = om.readValue(inputStream, RemoteConfigCollection.class);
        when(s3ConfigSource.getRemoteConfig()).thenReturn(remoteConfigCollection);
        when(localConfigRepository.getData(LOCAL_ONLY_ENTITY_ID)).thenReturn(Optional.of(localOnlyTransaction));
        when(localConfigRepository.getData(REMOTE_ENABLED_ENTITY_ID)).thenReturn(Optional.of(remoteEnabledTransaction));
        when(localConfigRepository.getData(REMOTE_DISABLED_ENTITY_ID)).thenReturn(Optional.of(remoteDisabledTransaction));
    }

    @Test
    public void getReturnsOptionalEmptyIfNoLocalConfigFound() {
        MatchingServiceConfigRepository configRepo = new MatchingServiceConfigRepository(localConfigRepository, s3ConfigSource);
        Optional<MatchingServiceConfig> result = configRepo.get(BAD_ENTITY_ID);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void getReturnsOptionalEmptyIfNoLocalConfigFoundButRemoteExists() {
        MatchingServiceConfigRepository configRepo = new MatchingServiceConfigRepository(localConfigRepository, s3ConfigSource);
        Optional<MatchingServiceConfig> result = configRepo.get(REMOTE_ONLY_ENTITY_ID);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void getReturnsLocalConfigWhenRemoteNotFound() {
        MatchingServiceConfigRepository configRepo = new MatchingServiceConfigRepository(localConfigRepository, s3ConfigSource);
        Optional<MatchingServiceConfig> result = configRepo.get(LOCAL_ONLY_ENTITY_ID);

        assertThat(result.get().getEntityId()).isEqualTo(LOCAL_ONLY_ENTITY_ID);
        assertThat(result.get().getEncryptionCertificate().getX509()).isEqualTo(localOnlyTransaction.getEncryptionCertificate().getX509());
    }

    @Test
    public void getReturnsLocalConfigWhenRemoteAvailableButDisabled() {
        MatchingServiceConfigRepository configRepo = new MatchingServiceConfigRepository(localConfigRepository, s3ConfigSource);
        Optional<MatchingServiceConfig> result = configRepo.get(REMOTE_DISABLED_ENTITY_ID);

        assertThat(result.get().getEntityId()).isEqualTo(REMOTE_DISABLED_ENTITY_ID);
        assertThat(result.get().getEncryptionCertificate().getX509()).isEqualTo(remoteDisabledTransaction.getEncryptionCertificate().getX509());
    }

    @Test
    public void getReturnsOverriddenConfigWhenRemoteFoundAndEnabled() {
        MatchingServiceConfigRepository configRepo = new MatchingServiceConfigRepository(localConfigRepository, s3ConfigSource);
        Optional<MatchingServiceConfig> result = configRepo.get(REMOTE_ENABLED_ENTITY_ID);

        assertThat(result.get().getEntityId()).isEqualTo(REMOTE_ENABLED_ENTITY_ID);
        assertThat(result.get().getEncryptionCertificate().getX509()).isEqualTo(REMOTE_CERT);
    }

    @Test
    public void getAllReturnsConfigAsPerIndividualRules(){

    }
}