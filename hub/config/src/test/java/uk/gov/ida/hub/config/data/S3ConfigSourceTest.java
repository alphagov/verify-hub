package uk.gov.ida.hub.config.data;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class S3ConfigSourceTest {

    private String awsRegion = System.getenv("awsRegion");
    private String bucketName = System.getenv("bucketName");
    private String objectKey = System.getenv("objectKey");

    private String selfServiceConfigJson = "{" +
                    "\"enabled\" : \"true\"," +
                    "\"awsRegion\": \"" + awsRegion + "\"," +
                    "\"s3BucketName\": \"" + bucketName + "\"," +
                    "\"s3ObjectKey\": \"" + objectKey + "\"" +
            "}";

    @Mock
    private AmazonS3 s3Client;

    @Mock
    private S3Object mockObject;

    @Mock
    private ConfigConfiguration configConfiguration;

    @Test
    /**
     * Tests to make sure we can process the JSON to an object
     */
    public void getRemoteConfigTest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        URL url = this.getClass().getResource("/remote-test-config.json");
        File initialFile = new File(url.getFile());
        InputStream testStream = new FileInputStream(initialFile);
        S3ObjectInputStream s3ObjectStream = new S3ObjectInputStream(testStream, new HttpGet());
        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigJson, SelfServiceConfig.class);
        when(configConfiguration.getSelfService()).thenReturn(selfServiceConfig);
        when(s3Client.getObject(any())).thenReturn(mockObject);
        when(mockObject.getObjectContent()).thenReturn(s3ObjectStream);
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, s3Client);
        RemoteConfigCollection result = testSource.getRemoteConfig();
        result.getPublishedAt();
        assertThat(result.getMatchingServiceAdapters().size()).isEqualTo(3);
        assertThat(result.getMatchingServiceAdapters().get("https://msa.bananaregistry.test.com").getName().contentEquals("Banana Registry MSA")).isTrue();
        assertThat(result.getMatchingServiceAdapters().get("https://msa.bananaregistry.test.com").getEncryptionCertificateConfig().getName().contentEquals("/C=uk/ST=London/O=GDS/CN=gds-msa-banana-encryption")).isTrue();
        assertThat(result.getMatchingServiceAdapters().get("https://msa.bananaregistry.test.com").getSigningCertificatesConfig().size()).isEqualTo(1);
        assertThat(result.getMatchingServiceAdapters().get("https://msa.bananaregistry.test.com").getSigningCertificatesConfig().get(0).getName().contentEquals("/C=uk/ST=London/O=GDS/CN=gds-msa-banana-signing")).isTrue();
        assertThat(result.getConnectedServices().size()).isEqualTo(3);
        assertThat(result.getConnectedServices().get("https://appleregistry.test.com").getServiceProviderConfig().getName().contentEquals("Apple Registry VSP")).isTrue();
        assertThat(result.getConnectedServices().get("https://appleregistry.test.com").getServiceProviderConfig().getSigningCertificatesConfig().size()).isEqualTo(1);
        assertThat(result.getConnectedServices().get("https://appleregistry.test.com").getServiceProviderConfig().getSigningCertificatesConfig().get(0).getName().contentEquals("/C=uk/ST=London/O=GDS/CN=gds-apple-signing")).isTrue();
        assertThat(result.getConnectedServices().get("https://bananaregistry.test.com").getServiceProviderConfig().getName().contentEquals("Banana Registry VSP")).isTrue();
        assertThat(result.getConnectedServices().get("https://bananaregistry.test.com").getServiceProviderConfig().getSigningCertificatesConfig().size()).isEqualTo(1);
        assertThat(result.getConnectedServices().get("https://bananaregistry.test.com").getServiceProviderConfig().getSigningCertificatesConfig().get(0).getName().contentEquals("/C=uk/ST=London/O=GDS/CN=gds-banana-signing")).isTrue();
        assertThat(result.getConnectedServices().get("https://cherryregistry.test.com").getServiceProviderConfig().getName().contentEquals("Banana Registry VSP")).isTrue();
        assertThat(result.getConnectedServices().get("https://cherryregistry.test.com").getServiceProviderConfig().getSigningCertificatesConfig().size()).isEqualTo(1);
        assertThat(result.getConnectedServices().get("https://cherryregistry.test.com").getServiceProviderConfig().getSigningCertificatesConfig().get(0).getName().contentEquals("/C=uk/ST=London/O=GDS/CN=gds-banana-signing")).isTrue();
    }

    @Test
    public void getRemoteConfigReturnsEmptyWhenS3Unavailable() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        URL url = this.getClass().getResource("/remote-test-config.json");
        File initialFile = new File(url.getFile());
        InputStream testStream = new FileInputStream(initialFile);
        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigJson, SelfServiceConfig.class);
        when(configConfiguration.getSelfService()).thenReturn(selfServiceConfig);
        when(s3Client.getObject(any())).thenReturn(mockObject);
        when(mockObject.getObjectContent()).thenReturn(null);
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, s3Client);
        RemoteConfigCollection result = testSource.getRemoteConfig();
        assertThat(result.getMatchingServiceAdapters().isEmpty()).isTrue();
        assertThat(result.getConnectedServices().isEmpty()).isTrue();
    }
}