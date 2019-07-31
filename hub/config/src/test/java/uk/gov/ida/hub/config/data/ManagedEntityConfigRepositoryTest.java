package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.SelfServiceMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
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

    private static final String REMOTE_CERT = "MIIDEjCCAfoCCQCRXwMPk5qw7zANBgkqhkiG9w0BAQsFADBLMQswCQYDVQQGEwJVSzEPMA0GA1UEBwwGTG9uZG9uMQwwCgYDVQQKDANHRFMxHTAbBgNVBAMMFEFwcGxlIFZTUCBFbmNyeXB0aW9uMB4XDTE5MDYyODE0MTkyOFoXDTM5MDYyODE0MTkyOFowSzELMAkGA1UEBhMCVUsxDzANBgNVBAcMBkxvbmRvbjEMMAoGA1UECgwDR0RTMR0wGwYDVQQDDBRBcHBsZSBWU1AgRW5jcnlwdGlvbjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMAi67JyfcMi/K/OZREDT1OwdKaTS78R6eFPrH5ZtmGZPRV37gp6cHsmC2qiS302pVCJ6g156MyALHllp2hHqSM80iM8vPvRQqZ0OZgdP4kzu/tBtPIoY1PpWMlaq21O7MJ1c5hj1RAev6W+vKFWVVmDFNNUYpt/LjaTno6Xv01NO8/3Fw+oW7/0c6Jod6Zfdd3UoGMV2YxdRu7xHkG4vba/R4LAiFr7rxzeO9eSCMdPm2f1ys7V4VjPFZqUliUxJMLW6E1hkmEjOGU6fmXClypFBQ4YIdxlARSgrzxNoPHEpFqVmGt5S4AK8QghvpbvtmOeHdAFh3OqieTcYn9BSeUCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEArrshCiwok8lCU/za5ip7zznMf9n9yViYTlxCEHvan+t11FCDz8f759zyQmM2HDtUL6yl71dEkHTvOm0D/PTBY//2s/+5duVU0AA2dTwg3L+vUB9itsBj1uq/kD3+FvePpuYBaS8ACLaRkIiNRxFj/KQWCQ5JK2oMODIsc6EwpXLEeSZmC1opa/rF+8vvb3zDo3dD7eWRmEe09yBPHkGm4dUGO5gi/zwkkjuUdmqB0qxdf2N1gfoOLb1x6nNw/YoBT86ZN4HICIW/CJS3huvG08tSJ06kGsu4znMdctzQ59TfgU3EYpy/j1XQGWKwh9gZxlXOHPjIfiE+kqTOj7clSQ==";

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
    public void setUp() throws Exception{
        URL url = this.getClass().getResource("/remote-test-config.json");
        File initialFile = new File(url.getFile());
        InputStream inputStream = new FileInputStream(initialFile);
        ObjectMapper om = new ObjectMapper();
        SelfServiceMetadata selfServiceMetadata = om.readValue(inputStream, SelfServiceMetadata.class);
        RemoteConfigCollection remoteConfigCollection = new RemoteConfigCollection(null, selfServiceMetadata);
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
        ManagedEntityConfigRepository<TransactionConfig> configRepo = new ManagedEntityConfigRepository<>(localConfigRepository, s3ConfigSource);
        Optional<TransactionConfig> result = configRepo.get(REMOTE_ONLY_ENTITY_ID);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void getReturnsLocalConfigWhenRemoteNotFound() {
        ManagedEntityConfigRepository<TransactionConfig> configRepo = new ManagedEntityConfigRepository<>(localConfigRepository, s3ConfigSource);
        Optional<TransactionConfig> result = configRepo.get(LOCAL_ONLY_ENTITY_ID);

        assertThat(result.get().getEntityId()).isEqualTo(LOCAL_ONLY_ENTITY_ID);
        assertThat(result.get().getEncryptionCertificate().getBase64Encoded()).isEqualTo(localOnlyTransaction.getEncryptionCertificate().getBase64Encoded());
    }

    @Test
    public void getReturnsLocalConfigWhenRemoteAvailableButDisabled() {
        ManagedEntityConfigRepository<TransactionConfig> configRepo = new ManagedEntityConfigRepository<>(localConfigRepository, s3ConfigSource);
        Optional<TransactionConfig> result = configRepo.get(REMOTE_DISABLED_ENTITY_ID);

        assertThat(result.get().getEntityId()).isEqualTo(REMOTE_DISABLED_ENTITY_ID);
        assertThat(result.get().getEncryptionCertificate().getBase64Encoded()).isEqualTo(remoteDisabledTransaction.getEncryptionCertificate().getBase64Encoded());
    }

    @Test
    public void getReturnsOverriddenConfigWhenRemoteFoundAndEnabled() {
        ManagedEntityConfigRepository<TransactionConfig>configRepo = new ManagedEntityConfigRepository<>(localConfigRepository, s3ConfigSource);
        Optional<TransactionConfig> result = configRepo.get(REMOTE_ENABLED_ENTITY_ID);

        assertThat(result.get().getEntityId()).isEqualTo(REMOTE_ENABLED_ENTITY_ID);
        assertThat(result.get().getEncryptionCertificate().getBase64Encoded().get()).isEqualTo(REMOTE_CERT);
    }

}