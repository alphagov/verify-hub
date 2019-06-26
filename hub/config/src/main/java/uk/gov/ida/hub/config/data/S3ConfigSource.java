package uk.gov.ida.hub.config.data;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConnectedServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteMatchingServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteServiceProviderConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.SelfServiceMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class S3ConfigSource {
    private static final Logger LOG = LoggerFactory.getLogger(S3ConfigSource.class);
    public static final Map<String, RemoteConnectedServiceConfig> EMPTY_CONNECTED_SERVICE_CONFIG_MAP = Collections.emptyMap();
    public static final Map<String, RemoteMatchingServiceConfig> EMPTY_MATCHING_SERVICE_CONFIG_MAP = Collections.emptyMap();
    public static final List<RemoteServiceProviderConfig> EMPTY_SERVICE_PROVIDER_CONFIG_LIST = Collections.emptyList();
    public static final RemoteConfigCollection EMPTY_COLLECTION = new RemoteConfigCollection(null, null, EMPTY_CONNECTED_SERVICE_CONFIG_MAP, EMPTY_MATCHING_SERVICE_CONFIG_MAP, EMPTY_SERVICE_PROVIDER_CONFIG_LIST);
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
            return EMPTY_COLLECTION;
        }
        try {
            RemoteConfigCollection config = cache.get(selfServiceConfig.getS3ObjectKey());
            return config;
        } catch (ExecutionException e) {
            LOG.warn("Unable to get {} from cache.", selfServiceConfig.getS3ObjectKey());
            return EMPTY_COLLECTION;
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
        public RemoteConfigCollection load(String key) throws Exception {
            S3Object s3Object = s3Client.getObject(bucket, key);
            try{
                return mapToRemoteConfigCollection(s3Object.getObjectContent(), s3Object.getObjectMetadata().getLastModified());
            } catch(IOException e) {
                LOG.error("An error occurred trying to get or process object {} from S3 Bucket {}", key, bucket, e);
                throw(e);
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
