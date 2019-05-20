package uk.gov.ida.hub.config.data;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class S3ConfigSource {
    private static final Logger LOG = LoggerFactory.getLogger(S3ConfigSource.class);
    private ConfigConfiguration configConfiguration;
    private AmazonS3 s3Client;

    public S3ConfigSource(ConfigConfiguration configConfiguration, AmazonS3 s3Client) {
        this.configConfiguration = configConfiguration;
        this.s3Client = s3Client;
    }

    /**
     * TODO: Refactor this static method out with guice later
     * @param configConfiguration
     * @return
     */
    public static S3ConfigSource setupS3ConfigSource(ConfigConfiguration configConfiguration) {
        AmazonS3 s3Client;
        if(!configConfiguration.getSelfService().getS3AccessKeyId().isEmpty() &&
                !configConfiguration.getSelfService().getS3SecretKeyId().isEmpty()) {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(configConfiguration.getSelfService().getS3AccessKeyId(),
                    configConfiguration.getSelfService().getS3SecretKeyId());
            s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(configConfiguration.getSelfService().getAwsRegion())
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .build();
        } else {
            s3Client = AmazonS3ClientBuilder.defaultClient();
        }
        return new S3ConfigSource(configConfiguration, s3Client);
    }

    public RemoteConfigCollection getRemoteConfig() throws IOException {
        S3Object fullObject = s3Client.getObject(
                new GetObjectRequest(configConfiguration.getSelfService().getS3BucketName(),
                configConfiguration.getSelfService().getS3ObjectKey()));
        InputStream s3ObjectStream =  fullObject.getObjectContent();
        ObjectMapper om = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        om.setDateFormat(df);
        try {
            return om.readValue(s3ObjectStream, RemoteConfigCollection.class);
        } catch(IOException e) {
            LOG.error("An error occured trying to get or process object {} from S3 Bucket {}",
                    configConfiguration.getSelfService().getS3ObjectKey(),
                    configConfiguration.getSelfService().getS3BucketName(), e);
            throw e;
        } finally {
            if(fullObject != null) {
                fullObject.close();
            }
        }
    }

}
