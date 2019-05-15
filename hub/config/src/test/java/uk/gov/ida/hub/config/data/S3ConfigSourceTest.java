package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class S3ConfigSourceTest {

    private String accessKey = System.getenv("awsAccessKey");
    private String secretKey = System.getenv("awsSecretKey");
    private String bucketName = System.getenv("bucketName");
    private String objectKey = System.getenv("objectKey");

    private String configJson = "{" +
                    "\"enabled\" : \"true\"," +
                    "\"s3BucketName\": \"" + bucketName + "\"," +
                    "\"s3ObjectKey\": \"" + objectKey + "\"," +
                    "\"s3AccessKeyId\": \"" + accessKey + "\"," +
                    "\"s3SecretKeyId\": \"" + secretKey + "\"" +
            "}";

    @Mock
    private ConfigConfiguration configConfiguration;

    @Test
    public void s3BuckConnectionTest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SelfServiceConfig selfServiceConfig = objectMapper.readValue(configJson, SelfServiceConfig.class);
        when(configConfiguration.getSelfService()).thenReturn(selfServiceConfig);
        S3ConfigSource testSource = new S3ConfigSource(configConfiguration);
        RemoteConfigCollection result = testSource.getRemoteConfig();
        assertThat(result.getEventId()).isEqualTo(76);
    }

}