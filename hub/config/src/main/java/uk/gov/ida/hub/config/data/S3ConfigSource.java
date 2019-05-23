package uk.gov.ida.hub.config.data;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class S3ConfigSource {
    private static final Logger LOG = LoggerFactory.getLogger(S3ConfigSource.class);
    private ConfigConfiguration configConfiguration;
    private long cacheLengthInMillis;
    private Date lastModifiedDate;
    private Long lastAccessedTime;
    private RemoteConfigCollection currentConfig;
    private AmazonS3 s3Client;

    public S3ConfigSource(ConfigConfiguration configConfiguration, AmazonS3 s3Client) {
        this.configConfiguration = configConfiguration;
        this.cacheLengthInMillis = configConfiguration.getSelfService().getCacheLengthInSeconds() * 1000;
        this.s3Client = s3Client;
    }

    /**
     * TODO: Refactor this static method out with guice later
     * @param configConfiguration
     * @return
     */
    public static S3ConfigSource setupS3ConfigSource(ConfigConfiguration configConfiguration) {
        AmazonS3 s3Client;
        if(configConfiguration.getSelfService().getS3AccessKeyId() == null ||
                configConfiguration.getSelfService().getS3SecretKeyId() == null) {
            s3Client = AmazonS3ClientBuilder.defaultClient();
        } else if(!configConfiguration.getSelfService().getS3AccessKeyId().isEmpty() &&
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
        try {
            // Object has never been retrieved get it
            if (lastModifiedDate == null) {
                lastAccessedTime = System.currentTimeMillis();
                getObjectData(getBucketObject());
            }
            // If the cache has timed out see if the object has changed
            else if (lastAccessedTime + cacheLengthInMillis < System.currentTimeMillis()) {
                lastAccessedTime = System.currentTimeMillis();
                S3Object s3Object = getBucketObject();
                // Get Object meta data... see if it has been changed.
                if (this.lastModifiedDate.before(s3Object.getObjectMetadata().getLastModified())) {
                    // If the object has changed get the object from s3
                    getObjectData(s3Object);
                }
            }
        } catch (IOException e) {
            if(this.currentConfig != null)
                return this.currentConfig;
            else
                LOG.error("A error has occurred... There is no cached remote config available, falling back to federated config");
                throw e;
        }

        return this.currentConfig;
    }

    /**
     * Get an sObject which makes a request for metadata only.
     * @return
     */
    private S3Object getBucketObject() {
        return s3Client.getObject(
                new GetObjectRequest(configConfiguration.getSelfService().getS3BucketName(),
                        configConfiguration.getSelfService().getS3ObjectKey()));
    }

    /**
     * This method gets the object data directly from the S3Bucket
     * @param s3Object
     * @throws IOException
     */
    private void getObjectData(S3Object s3Object) throws IOException {
        this.lastModifiedDate = s3Object.getObjectMetadata().getLastModified();
        S3ObjectInputStream s3ObjectStream = s3Object.getObjectContent();
        ObjectMapper om = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        om.setDateFormat(df);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(RemoteConfigCollection.class, new RemoteConfigCollectionDeserializer());
        om.registerModule(module);
        try {
            this.currentConfig = om.readValue(s3ObjectStream, RemoteConfigCollection.class);
        } catch(IOException e) {
            LOG.warn("An error occurred trying to get or process object {} from S3 Bucket {}",
                    configConfiguration.getSelfService().getS3ObjectKey(),
                    configConfiguration.getSelfService().getS3BucketName(), e);
            throw e;
        } finally {
            if(s3Object != null) {
               s3Object.close();
            }
        }
    }

    public void setCacheLengthInMillis(long cacheLengthInMillis) {
        this.cacheLengthInMillis = cacheLengthInMillis;
    }
}
