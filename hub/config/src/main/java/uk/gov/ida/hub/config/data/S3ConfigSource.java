package uk.gov.ida.hub.config.data;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.SelfServiceMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class S3ConfigSource {
    private static final Logger LOG = LoggerFactory.getLogger(S3ConfigSource.class);

    private boolean enabled = false;
    private String bucket;
    private SelfServiceConfig selfServiceConfig;
    private AmazonS3 s3Client;
    private CacheLoader<String, RemoteConfigCollection> cacheLoader;
    private LoadingCache<String, RemoteConfigCollection> cache;
    private ObjectMapper objectMapper;


    public S3ConfigSource() {
    }

    public S3ConfigSource(SelfServiceConfig selfServiceConfig, AmazonS3 s3Client, ObjectMapper objectMapper) {
        this.selfServiceConfig = selfServiceConfig;
        this.enabled = selfServiceConfig.isEnabled();
        this.bucket = selfServiceConfig.getS3BucketName();
        if (enabled && s3Client != null){
            this.s3Client = s3Client;
            this.objectMapper = objectMapper;
            this.cacheLoader = new S3ConfigCacheLoader();
            this.cache = CacheBuilder.newBuilder()
                    .refreshAfterWrite(selfServiceConfig.getCacheExpiry().toMilliseconds(), TimeUnit.MILLISECONDS)
                    .build(cacheLoader);
        }
    }

    public RemoteConfigCollection getRemoteConfig(){
        if (!enabled){
            return RemoteConfigCollection.EMPTY_REMOTE_CONFIG_COLLECTION;
        }
        try {
            RemoteConfigCollection config = cache.get(selfServiceConfig.getS3ObjectKey());
            return config;
        } catch (ExecutionException | UncheckedExecutionException e) {
            LOG.warn("Unable to get {} from cache.", selfServiceConfig.getS3ObjectKey());
            return RemoteConfigCollection.EMPTY_REMOTE_CONFIG_COLLECTION;
        }
    }

    private RemoteConfigCollection mapToRemoteConfigCollection(InputStream inputStream, Date lastModified) throws IOException {
        try {
            SelfServiceMetadata metadata = objectMapper.readValue(inputStream, SelfServiceMetadata.class);
            RemoteConfigCollection remoteConfig = new RemoteConfigCollection(lastModified, metadata);
            return remoteConfig;
        } finally {
            inputStream.close();
        }
    }

    public CacheLoader<String, RemoteConfigCollection> getCacheLoader() {
        return cacheLoader;
    }

    public class S3ConfigCacheLoader extends CacheLoader<String, RemoteConfigCollection> {

        @Override
        public RemoteConfigCollection load(String key)throws Exception {
            try{
                S3Object s3Object = s3Client.getObject(bucket, key);
                return mapToRemoteConfigCollection(s3Object.getObjectContent(), s3Object.getObjectMetadata().getLastModified());
            } catch (SdkClientException e) {
                LOG.error("An error occurred accessing object {} from S3 Bucket {}", key, bucket, e);
                return RemoteConfigCollection.EMPTY_REMOTE_CONFIG_COLLECTION;
            } catch (IOException e) {
                LOG.error("An error occurred trying to get or process object {} from S3 Bucket {}", key, bucket, e);
                return RemoteConfigCollection.EMPTY_REMOTE_CONFIG_COLLECTION;
            }
        }

        @Override
        public ListenableFuture<RemoteConfigCollection> reload(String key, RemoteConfigCollection existingConfig) {
            ListenableFutureTask<RemoteConfigCollection> task = ListenableFutureTask.create(() -> {
                S3Object s3Object = s3Client.getObject(
                        new GetObjectRequest(bucket, key)
                                .withModifiedSinceConstraint(existingConfig.getLastModified()));
                if (s3Object == null){
                    return existingConfig;
                }else {
                    return mapToRemoteConfigCollection(s3Object.getObjectContent(), s3Object.getObjectMetadata().getLastModified());
                }
            });
            new Thread(task).start();
            return task;
        }
    }
    
}
