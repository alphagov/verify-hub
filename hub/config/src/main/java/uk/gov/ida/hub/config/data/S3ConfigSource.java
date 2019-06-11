package uk.gov.ida.hub.config.data;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConnectedServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteMatchingServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteServiceProviderConfig;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
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
    public static final RemoteConfigCollection EMPTY_COLLECTION = new RemoteConfigCollection(null, EMPTY_CONNECTED_SERVICE_CONFIG_MAP, EMPTY_MATCHING_SERVICE_CONFIG_MAP, EMPTY_SERVICE_PROVIDER_CONFIG_LIST);
    private SelfServiceConfig selfServiceConfig;
    private AmazonS3 s3Client;
    private LoadingCache<String, RemoteConfigCollection> cache;

    public S3ConfigSource(ConfigConfiguration configConfiguration, AmazonS3 s3Client) {
        this.selfServiceConfig = configConfiguration.getSelfService();
        this.s3Client = s3Client;
        if (selfServiceConfig != null && selfServiceConfig.isEnabled()){
            CacheLoader<String, RemoteConfigCollection> cacheLoader = new S3ConfigCacheLoader();
            long expiry = selfServiceConfig.getCacheExpiryInSeconds();
            TimeUnit unit = TimeUnit.SECONDS;
            if (expiry == 0){
                expiry = 1;
                unit = TimeUnit.MILLISECONDS;
            }
            this.cache = CacheBuilder.newBuilder()
                    .refreshAfterWrite(expiry, unit)
                    .build(cacheLoader);
        }
    }

    public RemoteConfigCollection getRemoteConfig(){
        if ( selfServiceConfig == null || !selfServiceConfig.isEnabled()){
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

    private RemoteConfigCollection mapToRemoteConfigCollection(InputStream inputStream, Date lastModified) {
        ObjectMapper om = new ObjectMapper();
        om.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
        SimpleModule module = new SimpleModule();
        module.addDeserializer(RemoteConfigCollection.class, new RemoteConfigCollectionDeserializer());
        om.registerModule(module);
        try {
            RemoteConfigCollection remoteConfig = om.readValue(inputStream, RemoteConfigCollection.class);
            remoteConfig.setLastModified(lastModified);
            return remoteConfig;
        } catch(IOException e) {
            LOG.error("An error occurred trying to get or process object {} from S3 Bucket {}",
                    selfServiceConfig.getS3ObjectKey(),
                    selfServiceConfig.getS3BucketName(), e);
            throw new RuntimeException(e);
        } finally {
            try {
                inputStream.close();
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private S3Object getS3Object(String key) {
        return s3Client.getObject(
                new GetObjectRequest(selfServiceConfig.getS3BucketName(),
                        key));
    }

    private class S3ConfigCacheLoader extends CacheLoader<String, RemoteConfigCollection>{

        @Override
        public RemoteConfigCollection load(String key) {
            S3Object s3Object = getS3Object(key);
            return mapToRemoteConfigCollection(s3Object.getObjectContent(), s3Object.getObjectMetadata().getLastModified());
        }

        @Override
        public ListenableFuture<RemoteConfigCollection> reload(String key, RemoteConfigCollection existingConfig) {
            S3Object s3Object = getS3Object(key);
            Date lastModified = s3Object.getObjectMetadata().getLastModified();
            if (!existingConfig.getLastModified().before(lastModified)){
                return Futures.immediateFuture(existingConfig);
            }else {
                ListenableFutureTask<RemoteConfigCollection> task = ListenableFutureTask.create(() -> mapToRemoteConfigCollection(s3Object.getObjectContent(), lastModified));
                new Thread(task).start();
                return task;
            }
        }
    }
    
}
