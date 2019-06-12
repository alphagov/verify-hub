package uk.gov.ida.hub.config.data;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteMatchingServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteServiceProviderConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class S3ConfigSourceTest {

    private String selfServiceConfigEnabledJson = "{" +
                    "\"enabled\" : \"true\"," +
                    "\"cacheExpiryInSeconds\" : 5,"+
                    "\"s3BucketName\": \"s3BucketName\"," +
                    "\"s3ObjectKey\": \"s3ObjectName\"" +
            "}";

    private String selfServiceConfigDisabledJson = "{" +
            "\"enabled\" : \"false\"" +
            "}";

    private String selfServiceConfigShortCacheJson = "{" +
            "\"enabled\" : \"true\"," +
            "\"cacheExpiryInSeconds\" : 0,"+
            "\"s3BucketName\": \"s3BucketName\"," +
            "\"s3ObjectKey\": \"s3ObjectName\"" +
            "}";

    @Mock
    private AmazonS3 s3Client;

    @Mock
    private S3Object s3Object;

    @Mock
    private S3Object s3Object2;

    @Mock
    private ObjectMetadata objectMetadata;

    @Mock
    private ObjectMetadata objectMetadata2;

    @Mock
    private ConfigConfiguration configConfiguration;

    @Before
    public void setUp(){

    }

    @Test
    /**
     * Tests to make sure we can process the JSON to an object
     */
    public void getRemoteConfigReturnsRemoteConfigCollection() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigEnabledJson, SelfServiceConfig.class);
        when(configConfiguration.getSelfService()).thenReturn(selfServiceConfig);
        when(s3Client.getObject(any())).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(getObjectStream("/remote-test-config.json"));
        when(s3Object.getObjectMetadata()).thenReturn(objectMetadata);
        when(objectMetadata.getLastModified()).thenReturn(new Date());
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, s3Client);
        RemoteConfigCollection result = testSource.getRemoteConfig();
        Map<String, RemoteMatchingServiceConfig> msConfigs = result.getMatchingServiceAdapters();
        assertThat(msConfigs.size()).isEqualTo(3);
        assertThat(msConfigs.get("https://msa.bananaregistry.test.com").getName()).isEqualTo("Banana Registry MSA");
        assertThat(msConfigs.get("https://msa.bananaregistry.test.com").getEncryptionCertificate().getName()).isEqualTo("/C=uk/ST=London/O=GDS/CN=gds-msa-banana-encryption");
        assertThat(msConfigs.get("https://msa.bananaregistry.test.com").getSigningCertificates().size()).isEqualTo(1);
        assertThat(msConfigs.get("https://msa.bananaregistry.test.com").getSigningCertificates().get(0).getName()).isEqualTo("/C=uk/ST=London/O=GDS/CN=gds-msa-banana-signing");
        List<RemoteServiceProviderConfig> spConfigs = result.getServiceProviders();
        assertThat(spConfigs.size()).isEqualTo(2);
        assertThat(spConfigs.get(0).getName()).isEqualTo("Apple Registry VSP");
        assertThat(spConfigs.get(0).getSigningCertificates().size()).isEqualTo(1);
        assertThat(spConfigs.get(0).getSigningCertificates().get(0).getName()).isEqualTo("/C=uk/ST=London/O=GDS/CN=gds-apple-signing");
        assertThat(spConfigs.get(1).getName()).isEqualTo("Banana Registry VSP");
        assertThat(spConfigs.get(1).getSigningCertificates().size()).isEqualTo(1);
        assertThat(spConfigs.get(1).getSigningCertificates().get(0).getName()).isEqualTo("/C=uk/ST=London/O=GDS/CN=gds-banana-signing");
    }

    @Test
    public void getRemoteConfigReturnsEmptyRemoteConfigWhenSelfServiceDisabled() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigDisabledJson, SelfServiceConfig.class);
        when(configConfiguration.getSelfService()).thenReturn(selfServiceConfig);
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, null);
        RemoteConfigCollection result = testSource.getRemoteConfig();
        assertThat(result).isNotNull();
        assertThat(result.getServiceProviders().size()).isEqualTo(0);
        assertThat(result.getMatchingServiceAdapters().size()).isEqualTo(0);
        assertThat(result.getConnectedServices().size()).isEqualTo(0);
    }

    @Test
    public void getRemoteConfigReturnsCachedConfigWhenRepeatedlyCalled() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigEnabledJson, SelfServiceConfig.class);
        when(configConfiguration.getSelfService()).thenReturn(selfServiceConfig);
        when(s3Client.getObject(any())).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(getObjectStream("/remote-test-config.json"));
        when(s3Object.getObjectMetadata()).thenReturn(objectMetadata);
        when(objectMetadata.getLastModified()).thenReturn(new Date());
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, s3Client);
        RemoteConfigCollection result1 = testSource.getRemoteConfig();
        RemoteConfigCollection result2 = testSource.getRemoteConfig();
        verify(s3Object, times(1)).getObjectContent();
        assertThat(result1 == result2);

    }

    @Test
    public void getRemoteConfigOnlyRetrievesNewContentWhenLastModifiedChanges() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigShortCacheJson, SelfServiceConfig.class);
        when(configConfiguration.getSelfService()).thenReturn(selfServiceConfig);
        when(s3Client.getObject(any())).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(getObjectStream("/remote-test-config.json"));
        when(s3Object.getObjectMetadata()).thenReturn(objectMetadata);
        when(objectMetadata.getLastModified()).thenReturn(Date.from(Instant.now().minusMillis(10000)));
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, s3Client);
        RemoteConfigCollection result1 = testSource.getRemoteConfig();
        RemoteConfigCollection result2 = testSource.getRemoteConfig();
        assertThat((result1 == result2)).isTrue();
        verify(s3Object, times(1)).getObjectContent();
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        when(s3Client.getObject(any())).thenReturn(s3Object2);
        when(s3Object2.getObjectMetadata()).thenReturn(objectMetadata2);
        when(s3Object2.getObjectContent()).thenReturn(getObjectStream("/remote-test-config.json"));
        when(objectMetadata2.getLastModified()).thenReturn(Date.from(Instant.now()));

        testSource.getRemoteConfig();

        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(s3Object2, times(1)).getObjectContent();
        verify(objectMetadata2, times(1)).getLastModified();
    }

    private S3ObjectInputStream getObjectStream(String resource) throws FileNotFoundException {
        URL url = this.getClass().getResource(resource);
        File initialFile = new File(url.getFile());
        InputStream testStream = new FileInputStream(initialFile);
        return new S3ObjectInputStream(testStream, new HttpGet());
    }

}