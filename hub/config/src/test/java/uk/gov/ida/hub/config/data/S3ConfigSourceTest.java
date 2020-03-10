package uk.gov.ida.hub.config.data;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class S3ConfigSourceTest {


    private static final String CERT_MSA_BANANA_ENCRYPTION = "MIIDFDCCAfwCCQDEj/3MbRb8jzANBgkqhkiG9w0BAQsFADBMMQswCQYDVQQGEwJVSzEPMA0GA1UEBwwGTG9uZG9uMQwwCgYDVQQKDANHRFMxHjAcBgNVBAMMFUJhbmFuYSBNU0EgRW5jcnlwdGlvbjAeFw0xOTA2MjgxNDI0MzFaFw0zOTA2MjgxNDI0MzFaMEwxCzAJBgNVBAYTAlVLMQ8wDQYDVQQHDAZMb25kb24xDDAKBgNVBAoMA0dEUzEeMBwGA1UEAwwVQmFuYW5hIE1TQSBFbmNyeXB0aW9uMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1FuGXjWgeNNJ7a5todg/+gQO+xKUf6/tJ0wIW4dHS1lEOnk3mWu5fCtyTbDG9O+O22EOqmxMzsF6Kl/U6qRhmqs8bmc5pW9AQ67JMlMYCmrLq/VhF2aQ9rZV/Dx9rd2xuU6IkJPWWryY6qFfNrh6CDHzFzM5y+iGAXNLj1Z0TY8J38hjgRWCjSq9XD8tYW3SFfsonMRm71CvLGNl0WQu3WEGMu4yDqQjH8QT7LF1IF3obSeUPJKDnVIKa5/7THu/Lrekon8LJ5BbBYeBvahqpbQbvf2UK+lEvgCOUupGoPjz6mQ97tjHXCtE04xMyDEkMFy2urnNv2e2eVuy0VHE4wIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQCacP1D5haveoTdwhxnWnCwgc6TFiMd4g5Io31bXOvwShmqcYoJ7t9gdD7ZiPMJPbcF/YGCCk/BSEUtvYOPaRJV7C3BIZEPnewoQXyhX1uKzSqsYFIssl7DyUuItnmLZCQ4+OHpp1JMprDaWoF5hk2TdgqSv/fNlxt0193ayLzV+Dt34LhaS/pwXEBG/WtmJW3fygEOnmqmL4SMfG6nvvd/pOxAUeMEnzct3lJ5j2Qv/c0k43fUsy267gIRz/dpB/zlEzA6uUnrCNVdz+1AVjzvo9kf7H/4cA348mnBnh/USbRoIXhPkbPp5GuD3Q2CHvAL+bqVcQVNAJr6HKl+OwC4";
    private static final String CERT_MSA_BANANA_SIGNING = "MIIDDjCCAfYCCQCEmqzN+B9I0TANBgkqhkiG9w0BAQsFADBJMQswCQYDVQQGEwJVSzEPMA0GA1UEBwwGTG9uZG9uMQwwCgYDVQQKDANHRFMxGzAZBgNVBAMMEkJhbmFuYSBNU0EgU2lnbmluZzAeFw0xOTA2MjgxNDIzNDVaFw0zOTA2MjgxNDIzNDVaMEkxCzAJBgNVBAYTAlVLMQ8wDQYDVQQHDAZMb25kb24xDDAKBgNVBAoMA0dEUzEbMBkGA1UEAwwSQmFuYW5hIE1TQSBTaWduaW5nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwic0bJaHNqQNyZhFb2fE0ATFOWRO/DxDECeVLFsSyPbh0WUD4jVXJkvvSVK95DN1wdp63d0z02ErVgcMYaNnPI1Obpvl2MSWnJV33FGYOOCMDPgntigfRkrYVfTcEA4VmZ57r0tvmHGtCMUVo9CON9KA/FGBp1wnqLq89lQY2fmtk2wLxAWTjkcafKvkU2CLSrAZ6QAbJKCVMqeWyM2Fv6xxC2cUly+ygL/5wj21et9683tJDD3nAtt4wbfbYYXnGNCYJO86pK1Q3pZ+hLBDTmK0o73uVksqIFX64Qw5naYu9UztdgOZCNLwCfbdhFoThvmV+KWElHYTaSv38I91UwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBbu8Tmk3MuTS7kEHjxaQFSHIll1Ts9eM1cZarv2cNSayyvdevImf8MP3mtQKYtUTOaKlyYJ1MbI+Pi76NyyvUbCaeoP14R6FgSBe6fTrDgPiBe9+tIagBRkid0daV+h1S3M3Omwrvm/Ct7WfxbA+i4ioTHS6lLUgJVHxU1PyACrPdtdJfAk0pGmDEpm4rn9ZJYRhwfv4KiRf/bhxdcuvSwp5tQCFRwWzfoKoJF/54CKk/8Fo+oYqaNaiZ75/eaOCyXXsdvAFpLQpwn8OV6ASo1GisJL67PycSV6UMl8hGjz9ne8QlNz06Y/H+i4PJu1NLkM5QFShjhvywecuIzbqN3";
    private static final String CERT_VSP_APPLE_SIGNING = "MIIDDDCCAfQCCQCJ/3Eyv/MZMDANBgkqhkiG9w0BAQsFADBIMQswCQYDVQQGEwJVSzEPMA0GA1UEBwwGTG9uZG9uMQwwCgYDVQQKDANHRFMxGjAYBgNVBAMMEUFwcGxlIFZTUCBTaWduaW5nMB4XDTE5MDYyODE0MTMzM1oXDTM5MDYyODE0MTMzM1owSDELMAkGA1UEBhMCVUsxDzANBgNVBAcMBkxvbmRvbjEMMAoGA1UECgwDR0RTMRowGAYDVQQDDBFBcHBsZSBWU1AgU2lnbmluZzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAN0MGCJFJ1nRvgD3Scgr3TdK365pvHp0i8x5G/XmVnXU8sdrr2EgY27UV6CNJI95aE5Tt90JHr/XCOTEdY9zsu5wCVizx7o3g/6LsrTNGDkiz8pLuvygOm2zr32m70qw2IXtuXjLEJwhqcWjWOL5Tx7szFrVfClXhQcCQwuYFe57449/makLqPSY9pUYAX9juaDYjcMjTtShJ9AVov3Z7j0KgAVNRLFG97EiniD0vglxs7L6wguU3QP0PC0iTSk83zTxoho76bcFDZDoFWs3LhLALUOchntjeSBbWC61EzIVWp7kA0JM6Kg8FW7zljE7MG+yVy+Qggqmxo+pTBi2LqkCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAF0MnpSXv0A0UJ+l2MN3A9kbg8T6m92blqiPrwnaTsj7tLAOCk6CY7V5X5No2LnQ5TB2mOzsaQ3a/YGP6TtaYaCnyZ4bkqVuviAwwNPj7QiKEJIi9/sVUcmVne/qPUozrcM/sz4L8+GhrgZjAwWWqDnEpdybQjqaOMB7kbTMiOL0psYDy7FRpU79+usUTns4ZQw21ucFRE10eMdbYkQYsHYM1TpzXhJnJ2YY4pSUgbHEibQoBAcK0YUErJDmUFukfsfa29T9DMS3R3RHeQQIBpEwTiThUxfAsrVX2R1xqoylLN9nhEi5Nhv+/n6ZZxN3FGw2ebzavowbDCsuyJxVPyg==";
    private static final String CERT_VSP_BANANA_SIGNING = "MIIDDjCCAfYCCQC4nnLu8fPLIjANBgkqhkiG9w0BAQsFADBJMQswCQYDVQQGEwJVSzEPMA0GA1UEBwwGTG9uZG9uMQwwCgYDVQQKDANHRFMxGzAZBgNVBAMMEkJhbmFuYSBWU1AgU2lnbmluZzAeFw0xOTA2MjgxNDIyMjZaFw0zOTA2MjgxNDIyMjZaMEkxCzAJBgNVBAYTAlVLMQ8wDQYDVQQHDAZMb25kb24xDDAKBgNVBAoMA0dEUzEbMBkGA1UEAwwSQmFuYW5hIFZTUCBTaWduaW5nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3JmFyYO0pF6krivSlsJgpe9AMmr88FmUpShUquNUvQIe6tQtPp436azXLozlZXyPR+nJ6EfE6H/m5jhobwOuq7EGRKU2pjEwveTLB1qI1NOUCEBkv5ii9Bm+3xkiF840U15D8ftjr20VLcCDuyI/bbLqS4rDC7syaSeD6SN7k4ifRhIyfxaXBHts7m++6zKENisH+2laQ/GYNP2/TZHXvxS2CWXGgw5RDhY7LbOEZldU/PPlW8WkqCDgUFEdRa65Pe6c22zsPYMD+JsuIkW0bE0uceAY6ja5sNyEBkEZe/1A409O6+q6OhyTAtJ5ewcNSIx4L0eh5MM5AQ4zFTJRuwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBP4WBZufGxcGoKvZnyTTbprpbEA/pILC0pNK/7Dku3m1zR8zEpSehnbaWKWO9KpSvuak1Kp983BdczcJy/0zSml+RkN46yia65eebZniWFtJ32/TgpW2ALbqLjVCfr4h5OinPouf4yKCDpSvb8CtFsHCeHDh9EdWtoZzDgeFJqPADwP9/A6asyKIOVag5QuCUqlEpr7lcAWKWYb7eQsL105WdssOQF1R/W70x4TCbK72U0t3pRbBEab10JrjUpPscc+NnEKgz33zILTXjEO1FeEcdTGcs5AvwR2GIwAg2IBGZY/USyD6LKmvHS3tvyxWbM9qnw00Fr3Cl15ZpWzClK";

    private static final String BUCKET_NAME = "s3BucketName";
    private static final String OBJECT_KEY = "s3ObjectName";

    private String selfServiceConfigEnabledJson = "{" +
            "   \"enabled\" : \"true\"," +
            "   \"cacheExpiry\" : \"5s\","+
            "   \"s3BucketName\": \""+BUCKET_NAME+"\"," +
            "   \"s3ObjectKey\": \""+OBJECT_KEY+"\"" +
            "}";

    private String selfServiceConfigDisabledJson = "{" +
            "   \"enabled\" : \"false\"" +
            "}";

    private String selfServiceConfigShortCacheJson = "{" +
            "   \"enabled\" : \"true\"," +
            "   \"cacheExpiry\" : \"1ms\","+
            "   \"s3BucketName\": \""+BUCKET_NAME+"\"," +
            "   \"s3ObjectKey\": \""+OBJECT_KEY+"\"" +
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

    private ObjectMapper objectMapper;

    @Before
    public void setUp(){
        objectMapper = new ObjectMapper();
    }

    @Test
    /**
     * Tests to make sure we can process the JSON to an object
     */
    public void getRemoteConfigReturnsRemoteConfigCollection() throws Exception {
        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigEnabledJson, SelfServiceConfig.class);
        when(s3Client.getObject(new GetObjectRequest(BUCKET_NAME, OBJECT_KEY))).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(getObjectStream("/remote-test-config.json"));
        when(s3Object.getObjectMetadata()).thenReturn(objectMetadata);
        when(objectMetadata.getLastModified()).thenReturn(new Date());
        S3ConfigSource testSource = new S3ConfigSource(selfServiceConfig, s3Client, objectMapper);
        RemoteConfigCollection result = testSource.getRemoteConfig();
        Map<String, RemoteMatchingServiceConfig> msConfigs = result.getMatchingServiceAdapters();
        assertThat(msConfigs.size()).isEqualTo(3);
        assertThat(msConfigs.get("https://msa.bananaregistry.test.com").getName()).isEqualTo("Banana Registry MSA");
        assertThat(msConfigs.get("https://msa.bananaregistry.test.com").getEncryptionCertificate()).contains(CERT_MSA_BANANA_ENCRYPTION);
        assertThat(msConfigs.get("https://msa.bananaregistry.test.com").getSignatureVerificationCertificates().size()).isEqualTo(1);
        assertThat(msConfigs.get("https://msa.bananaregistry.test.com").getSignatureVerificationCertificates().get(0)).contains(CERT_MSA_BANANA_SIGNING);
        Map<String, RemoteServiceProviderConfig> spConfigs = result.getServiceProviders();
        assertThat(spConfigs.size()).isEqualTo(3);
        RemoteServiceProviderConfig spConfig2 = spConfigs.get("2");
        assertThat(spConfig2.getName()).isEqualTo("Apple Registry VSP");
        assertThat(spConfig2.getSignatureVerificationCertificates().size()).isEqualTo(1);
        assertThat(spConfig2.getSignatureVerificationCertificates().get(0)).contains(CERT_VSP_APPLE_SIGNING);
        RemoteServiceProviderConfig spConfig3 = spConfigs.get("3");
        assertThat(spConfig3.getName()).isEqualTo("Banana Registry VSP");
        assertThat(spConfig3.getSignatureVerificationCertificates().size()).isEqualTo(1);
        assertThat(spConfig3.getSignatureVerificationCertificates().get(0)).contains(CERT_VSP_BANANA_SIGNING);
    }

    @Test
    public void getRemoteConfigReturnsEmptyRemoteConfigWhenSelfServiceDisabled() throws IOException {
        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigDisabledJson, SelfServiceConfig.class);
        S3ConfigSource testSource = new S3ConfigSource(selfServiceConfig, null, objectMapper);
        RemoteConfigCollection result = testSource.getRemoteConfig();
        assertThat(result).isNotNull();
        assertThat(result.getServiceProviders().size()).isEqualTo(0);
        assertThat(result.getMatchingServiceAdapters().size()).isEqualTo(0);
        assertThat(result.getConnectedServices().size()).isEqualTo(0);
    }

    @Test
    public void getRemoteConfigReturnsCachedConfigWhenRepeatedlyCalled() throws IOException {
        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigEnabledJson, SelfServiceConfig.class);
        when(s3Client.getObject(new GetObjectRequest(BUCKET_NAME, OBJECT_KEY))).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(getObjectStream("/remote-test-config.json"));
        when(s3Object.getObjectMetadata()).thenReturn(objectMetadata);
        when(objectMetadata.getLastModified()).thenReturn(new Date());
        S3ConfigSource testSource = new S3ConfigSource(selfServiceConfig, s3Client, objectMapper);
        RemoteConfigCollection result1 = testSource.getRemoteConfig();
        RemoteConfigCollection result2 = testSource.getRemoteConfig();
        verify(s3Object, times(1)).getObjectContent();
        assertThat(result1 == result2);

    }

    @Test
    public void getRemoteConfigOnlyRetrievesNewContentWhenLastModifiedChanges() throws Exception {
        SelfServiceConfig selfServiceConfig = objectMapper.readValue(selfServiceConfigShortCacheJson, SelfServiceConfig.class);
        when(s3Client.getObject(new GetObjectRequest(BUCKET_NAME, OBJECT_KEY))).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(getObjectStream("/remote-test-config.json"));
        when(s3Object.getObjectMetadata()).thenReturn(objectMetadata);
        when(objectMetadata.getLastModified()).thenReturn(Date.from(Instant.now().minusMillis(10000)));
        S3ConfigSource testSource = new S3ConfigSource(selfServiceConfig, s3Client, objectMapper);
        var testCacheLoader = testSource.getCacheLoader();
        RemoteConfigCollection result1 = testSource.getRemoteConfig();
        ListenableFuture<RemoteConfigCollection> task = testCacheLoader.reload("test", result1);
        while(!task.isDone()){
            Thread.yield();
        }
        RemoteConfigCollection result2 = task.get();
        assertThat((result1 == result2)).isTrue();
        verify(s3Object, times(1)).getObjectContent();
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object2);
        when(s3Object2.getObjectMetadata()).thenReturn(objectMetadata2);
        when(s3Object2.getObjectContent()).thenReturn(getObjectStream("/remote-test-config.json"));
        when(objectMetadata2.getLastModified()).thenReturn(Date.from(Instant.now()));
        ListenableFuture<RemoteConfigCollection> task2 = testCacheLoader.reload("test", result1);
        while(!task2.isDone()){
            Thread.yield();
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
