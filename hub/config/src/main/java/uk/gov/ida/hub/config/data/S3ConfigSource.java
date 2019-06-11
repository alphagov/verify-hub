package uk.gov.ida.hub.config.data;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class S3ConfigSource {
    private static final Logger LOG = LoggerFactory.getLogger(S3ConfigSource.class);
    private ConfigConfiguration configConfiguration;
    private AmazonS3 s3Client;
    private LoadingCache<String, RemoteConfigCollection> cache;

    public S3ConfigSource(ConfigConfiguration configConfiguration, AmazonS3 s3Client) {
        this.configConfiguration = configConfiguration;
        this.s3Client = s3Client;

        CacheLoader<String, RemoteConfigCollection> loader;
        loader = new CacheLoader<String, RemoteConfigCollection>() {
            private RemoteConfigCollection currentConfig;
            private Date lastModified;

            @Override
            public RemoteConfigCollection load(String key) {
                S3Object s3Object = getS3Object(key);
                if (this.lastModified.before(s3Object.getObjectMetadata().getLastModified())) {
                    // If the object has changed get the object from s3
                    updateObjectData(s3Object);
                }
                return this.currentConfig;
            }

            private S3Object getS3Object(String key) {
                return s3Client.getObject(
                        new GetObjectRequest(configConfiguration.getSelfService().getS3BucketName(),
                                key));
            }

            private void updateObjectData(S3Object s3Object) {
                InputStream s3ObjectStream =  s3Object.getObjectContent();
                ObjectMapper om = new ObjectMapper();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                om.setDateFormat(df);
                SimpleModule module = new SimpleModule();
                module.addDeserializer(RemoteConfigCollection.class, new RemoteConfigCollectionDeserializer());
                om.registerModule(module);
                try {
                    this.currentConfig = om.readValue(s3ObjectStream, RemoteConfigCollection.class);
                    this.lastModified = s3Object.getObjectMetadata().getLastModified();
                } catch(IOException e) {
                    LOG.error("An error occured trying to get or process object {} from S3 Bucket {}",
                            configConfiguration.getSelfService().getS3ObjectKey(),
                            configConfiguration.getSelfService().getS3BucketName(), e);
                    throw new RuntimeException(e);
                } finally {
                    if(s3Object != null) {
                        try {
                            s3Object.close();
                        } catch(IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };

        this.cache = CacheBuilder.newBuilder()
                .expireAfterAccess(configConfiguration.getSelfService().getCacheExpiryInSeconds(), TimeUnit.SECONDS)
                .build(loader);
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
        try {
            return cache.get(configConfiguration.getSelfService().getS3ObjectKey());
        } catch (ExecutionException e) {
            LOG.warn("Unable to get {} from Guava cache.", configConfiguration.getSelfService().getS3ObjectKey());
            throw new IOException(e);
        }
    }

}
