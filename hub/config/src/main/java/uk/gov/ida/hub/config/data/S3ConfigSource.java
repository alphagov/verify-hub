package uk.gov.ida.hub.config.data;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3DataSource;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Configure S3 connection here
 */
public class S3ConfigSource{

    private S3DataSource s3DataSource;
    private ConfigConfiguration configConfiguration;
    private AmazonS3 s3Client;

    public S3ConfigSource(ConfigConfiguration configConfiguration){
        this.configConfiguration = configConfiguration;
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(configConfiguration.getSelfService().getS3AccessKeyId(),
                configConfiguration.getSelfService().getS3SecretKeyId());
        this.s3Client = AmazonS3ClientBuilder.standard().withRegion("eu-west-2")
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }

    public RemoteConfigCollection getRemoteConfig() throws IOException {
        S3Object fullObject;
        fullObject = s3Client.getObject(new GetObjectRequest(configConfiguration.getSelfService().getS3BucketName(),
                configConfiguration.getSelfService().getS3ObjectKey()));
        InputStream s3ObjectStream =  fullObject.getObjectContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectStream));
        StringBuilder configFile = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            configFile.append(line);
            System.out.println(line);
        }
        ObjectMapper om = new ObjectMapper();
        return om.readValue(configFile.toString(), RemoteConfigCollection.class);
    }

}
