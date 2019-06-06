package uk.gov.ida.hub.config.data;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.jackson.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.remoteconfig.ConnectedService;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConnectedServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteMatchingServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteServiceProviderConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.SelfServiceMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class S3ConfigSource {
    private static final Logger LOG = LoggerFactory.getLogger(S3ConfigSource.class);
    public static final RemoteConfigCollection EMPTY_COLLECTION = new RemoteConfigCollection(null, ImmutableMap.of(), ImmutableMap.of());
    private ConfigConfiguration configConfiguration;
    private final AmazonS3 s3Client;
    private final ObjectMapper om;

    public S3ConfigSource(ConfigConfiguration configConfiguration, AmazonS3 s3Client) {
        this.configConfiguration = configConfiguration;
        this.s3Client = s3Client;
        om = Jackson.newObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        om.setDateFormat(df);
    }

    public static S3ConfigSource setupS3ConfigSource(ConfigConfiguration configConfiguration) {
        if(configConfiguration.getSelfService() == null || !configConfiguration.getSelfService().isEnabled()) {
            return nullS3ConfigSource();
        }
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

    private static S3ConfigSource nullS3ConfigSource() {
        return new S3ConfigSource(null, null) {
            @Override
            public RemoteConfigCollection getRemoteConfig() {
                LOG.error("S3 Config was requested but feature is disabled.");
                return S3ConfigSource.EMPTY_COLLECTION;
            }
        };
    }

    public RemoteConfigCollection getRemoteConfig() {
        Optional<SelfServiceMetadata> jsonObject = getJsonObject(SelfServiceMetadata.class);
        return jsonObject.map(this::convertToRemoteConfigCollection).orElse(EMPTY_COLLECTION);
    }

    private Optional<SelfServiceMetadata> getJsonObject(Class<SelfServiceMetadata> valueType) {
        S3Object fullObject = null;
        try {
            fullObject = s3Client.getObject(
                new GetObjectRequest(configConfiguration.getSelfService().getS3BucketName(),
                    configConfiguration.getSelfService().getS3ObjectKey()));
            InputStream s3ObjectStream = fullObject.getObjectContent();
            SelfServiceMetadata selfServiceMetadata = om.readValue(s3ObjectStream, valueType);
            return Optional.of(selfServiceMetadata);
        } catch(IOException | SdkClientException e) {
            LOG.error(
                String.format(
                    "An error occured trying to get or process object %s from S3 Bucket %s",
                    configConfiguration.getSelfService().getS3ObjectKey(),
                    configConfiguration.getSelfService().getS3BucketName()
                ), e);
            return Optional.empty();
        } finally {
            if(fullObject != null) {
                try {
                    fullObject.close();
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private RemoteConfigCollection convertToRemoteConfigCollection(SelfServiceMetadata selfServiceMetadata) {
        return new RemoteConfigCollection(
            selfServiceMetadata.getPublishedAt(),
            createConnectedServiceConfig(selfServiceMetadata),
            createMatchingServiceConfigMap(selfServiceMetadata)
        );
    }

    private Map<String, ConnectedService> createConnectedServiceConfig(SelfServiceMetadata selfServiceMetadata) {
        Map<Integer, RemoteServiceProviderConfig> serviceProviderConfigMap = createServiceProviderConfigMap(selfServiceMetadata);

        return selfServiceMetadata.getConnectedServices()
            .stream()
            .map((service) -> serviceProviderConfigForService(serviceProviderConfigMap, service))
            .filter(Optional::isPresent).map(Optional::get)
            .collect(Collectors.toMap(ConnectedService::getEntityId, Function.identity())
            );
    }

    private Optional<ConnectedService> serviceProviderConfigForService(Map<Integer, RemoteServiceProviderConfig> serviceProviderConfigMap, RemoteConnectedServiceConfig service) {
        return Optional.ofNullable(serviceProviderConfigMap.get(service.getServiceProviderConfigId()))
            .map((serviceProviderConfig) -> new ConnectedService(service, serviceProviderConfig)
            ).or(() -> {
                LOG.error(
                    "Unable to locate service provider config for {} with id: {}",
                    service.getEntityId(),
                    service.getServiceProviderConfigId()
                );
                return Optional.empty();
            });
    }

    private Map<Integer, RemoteServiceProviderConfig> createServiceProviderConfigMap(SelfServiceMetadata selfServiceMetadata) {
        return selfServiceMetadata.getServiceProviders().stream()
            .collect(Collectors.toMap(
                RemoteServiceProviderConfig::getId,
                Function.identity()
            ));
    }

    private Map<String, RemoteMatchingServiceConfig> createMatchingServiceConfigMap(SelfServiceMetadata selfServiceMetadata) {
        return selfServiceMetadata.getMatchingServiceAdapters().stream()
            .collect(Collectors.toMap(
                RemoteMatchingServiceConfig::getEntityId,
                Function.identity()
            ));
    }

}
