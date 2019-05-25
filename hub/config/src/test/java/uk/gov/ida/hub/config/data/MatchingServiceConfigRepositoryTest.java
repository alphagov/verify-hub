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
    private static final String REMOTE_ONLY_ENTITY_ID = "https://msa.bananaregistry.service.gov.uk";
    private static final String REMOTE_ENABLED_ENTITY_ID = "https://msa.appleregistry.service.gov.uk";
    private static final String REMOTE_DISABLED_ENTITY_ID = "https://msa.cherryregistry.service.gov.uk";
    private static final String LOCAL_ONLY_ENTITY_ID = "https://msa.local.service.gov.uk";
    private static final String BAD_ENTITY_ID = "http://msa.none.existent.service.gov.uk";

    private static final String REMOTE_CERT = "MIIDQTCCAiigAwIBAgIBADANBgkqhkiG9w0BAQ0FADA6MQswCQYDVQQGEwJ1azEPMA0GA1UECAwGTG9uZG9uMQwwCgYDVQQKDANHRFMxDDAKBgNVBAMMA2dkczAeFw0xOTA1MTYxNDAyMzBaFw0yMDA1MTUxNDAyMzBaMDoxCzAJBgNVBAYTAnVrMQ8wDQYDVQQIDAZMb25kb24xDDAKBgNVBAoMA0dEUzEMMAoGA1UEAwwDZ2RzMIIBIzANBgkqhkiG9w0BAQEFAAOCARAAMIIBCwKCAQIA078oF47ZG5ETMQcVxVVV5yXvew6kYkHPgyWe2gpmXTuUDOYJv84xuQ5QJvAT3yPL+s5k6fV17Cdt1oHHnb8L+05jspKNbPq6Tmg6f+1HOKVrq3CFcrLo6+ETBDybrC+GFnoygrZKDuyi3BXiPsd8WfsOk/TI47d6ib8QUmK10uFDbK7o53FXpqrZ5bAGimm4mKeR/nTO7zvVrvSecYMJmvCe2yvUXvefmDVArDT0YgX93IvQ1BQ5VG+gIOTPbXJa4700t/zw9vehZumzbU2DUTM+qGakhhFZp7Txb3LxwLgLK7jPaonvtIOX5dhxLD2ijMfwxVG2Bfas7EMC+nB909UCAwEAAaNQME4wHQYDVR0OBBYEFB1LHW9K6Kq5fRDQL/6sP4ExLKUzMB8GA1UdIwQYMBaAFB1LHW9K6Kq5fRDQL/6sP4ExLKUzMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQENBQADggECABK2qsX/AU90FFN85W2uQEw2sr0j2WZnB8eFjy1IRe99W6t4gLAPcMV6JLWMcee85sYDqNnZ2DpXVFPbdQIAEgb5nKsxVhvtXNjOATeRT8QOauUnRA+Sj5UAZdXmhVYS1hp9Wj6pEF9C/Oo5LsO0qtsYQT+EEYptZPhAv5Hw2zPCTpN5lhmwwiw43Q9YD98B9FJDIEHxldi86B6rcX/Kyt3I+nqKFuwW9ffKSdgaDxQJS/kXqvDz1bIp19w5Yodkv7yP38z0qIIuZkgIMTXANyNiauHU1jrWcUaZUjvfBzs05DIBymDnKt2XJ5bs0li6hlq4+n8vt9McJv5IBxNr9hdZ";

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
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void getReturnsOptionalEmptyIfNoLocalConfigFoundButRemoteExists() {
        MatchingServiceConfigRepository configRepo = new MatchingServiceConfigRepository(localConfigRepository, s3ConfigSource);
        Optional<MatchingServiceConfig> result = configRepo.get(REMOTE_ONLY_ENTITY_ID);
        assertThat(result.isEmpty()).isTrue();
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