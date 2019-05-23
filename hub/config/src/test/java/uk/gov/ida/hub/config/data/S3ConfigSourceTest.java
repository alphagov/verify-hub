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
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteCertificateConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

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
    private ObjectMetadata mockMetaData;

    @Mock
    private ConfigConfiguration configConfiguration;

    @Before
    public void setup() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigJson, SelfServiceConfig.class);
        when(configConfiguration.getSelfService()).thenReturn(selfServiceConfig);
        when(s3Client.getObject(any())).thenReturn(mockObject);
        when(mockObject.getObjectMetadata()).thenReturn(mockMetaData);
        when(mockMetaData.getLastModified()).thenReturn(new Date(System.currentTimeMillis()));
        when(mockObject.getObjectContent()).thenReturn(getObjectStream("/remote-test-config.json"));
    }

    private S3ObjectInputStream getObjectStream(String filename) throws IOException {
        URL url = this.getClass().getResource(filename);
        File initialFile = new File(url.getFile());
        InputStream testStream = new FileInputStream(initialFile);
        return new S3ObjectInputStream(testStream, new HttpGet());
    }


    @Test
    /**
     * Tests to make sure we can process the JSON to an object
     */
    public void getRemoteConfigTest() throws Exception {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date expectedDate = df.parse("2019-05-16T14:35:52.991+00:00");
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, s3Client);
        RemoteConfigCollection result = testSource.getRemoteConfig();
        result.getPublishedAt();
        assertThat(result.getPublishedAt()).isEqualTo(expectedDate);
        assertThat(result.getMatchingServiceAdapters().size()).isEqualTo(1);
        assertThat(result.getMatchingServiceAdapters().get("https://msa.bananaregistry.service.gov.uk").getName().contentEquals("Banana Registry MSA")).isTrue();
        assertThat(result.getMatchingServiceAdapters().get("https://msa.bananaregistry.service.gov.uk").getEncryptionCertificate().getName().contentEquals("/C=uk/ST=London/O=GDS/CN=gds")).isTrue();
        assertThat(result.getMatchingServiceAdapters().get("https://msa.bananaregistry.service.gov.uk").getSigningCertificates().size()).isEqualTo(1);
        assertThat(result.getMatchingServiceAdapters().get("https://msa.bananaregistry.service.gov.uk").getSigningCertificates().get(0).getName().contentEquals("/C=uk/ST=London/O=GDS/CN=gds")).isTrue();
        assertThat(result.getServiceProviders().size()).isEqualTo(2);
        assertThat(result.getServiceProviders().get(0).getName().contentEquals("chris")).isTrue();
        assertThat(result.getServiceProviders().get(0).getSigningCertificates().size()).isEqualTo(1);
        assertThat(result.getServiceProviders().get(0).getSigningCertificates().get(0).getName().contentEquals("/C=UK/O=DEFRA/CN=www-chs-perf.ruraldev.org.uk-MSA-SAML-ENCRYPT-INT-120319")).isTrue();
        assertThat(result.getServiceProviders().get(1).getName().contentEquals("Banana Registry VSP")).isTrue();
        assertThat(result.getServiceProviders().get(1).getSigningCertificates().size()).isEqualTo(1);
        assertThat(result.getServiceProviders().get(1).getSigningCertificates().get(0).getName().contentEquals("/C=GB/ST=London/L=London/O=Cabinet Office/OU=GDS/CN=HUB Signing (20190218155358)")).isTrue();
    }

    @Test
    public void cacheTest()  throws Exception {
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, s3Client);
        testSource.setCacheLengthInMillis(100);
        // Make First request
        RemoteConfigCollection result = testSource.getRemoteConfig();
        Date initialPublishedDate = result.getPublishedAt();
        // Update source file and modified time
        when(mockObject.getObjectContent()).thenReturn(getObjectStream("/remote-test-config-mod.json"));
        when(mockMetaData.getLastModified()).thenReturn(new Date(System.currentTimeMillis()));
        // Wait
        Thread.sleep(7);
        // Make second request, if we read from our cache the published date doesn't change
        result = testSource.getRemoteConfig();
        // Test
        assertThat(result.getPublishedAt()).isEqualTo(initialPublishedDate);
    }

    @Test
    public void cacheExpireTest()  throws Exception {
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, s3Client);
        testSource.setCacheLengthInMillis(10);
        // Make first request
        RemoteConfigCollection result = testSource.getRemoteConfig();
        Date initialPublishedDate = result.getPublishedAt();
        // Update source file and modified time
        when(mockObject.getObjectContent()).thenReturn(getObjectStream("/remote-test-config-mod.json"));
        when(mockMetaData.getLastModified()).thenReturn(new Date(System.currentTimeMillis()));
        // Wait
        Thread.sleep(15);
        // Make second request, if we read from our cache the published date should change
        result = testSource.getRemoteConfig();
        // Test
        assertThat(result.getPublishedAt()).isNotEqualTo(initialPublishedDate);
    }

    @Test
    public void checkCacheReturnsCacheObjectOnIOError()  throws Exception {
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration, s3Client);
        testSource.setCacheLengthInMillis(10);
        // Make first request
        RemoteConfigCollection result = testSource.getRemoteConfig();
        Date initialPublishedDate = result.getPublishedAt();
        // Update source file to be empty to trigger an exception and modified time
        when(mockObject.getObjectContent()).thenReturn(new S3ObjectInputStream(new ByteArrayInputStream( "".getBytes()), new HttpGet()));
        when(mockMetaData.getLastModified()).thenReturn(new Date(System.currentTimeMillis()));
        // Wait
        Thread.sleep(15);
        // Make second request, if we read from our cache the published date should be the same
        result = testSource.getRemoteConfig();
        // Test
        assertThat(result.getPublishedAt()).isEqualTo(initialPublishedDate);
    }



}