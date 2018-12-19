package uk.gov.ida.hub.samlengine.config;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Singleton;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.annotations.Config;
import uk.gov.ida.hub.samlengine.domain.CertificateDto;
import uk.gov.ida.hub.samlengine.domain.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Singleton
public class ConfigProxy {

    private final JsonClient jsonClient;
    private final URI configUri;

    private LoadingCache<URI, CertificateDto> encryptionCertificates = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<URI, CertificateDto>() {
                @Override
                public CertificateDto load(URI key) {
                    return jsonClient.get(key, CertificateDto.class);
                }
            });
    private LoadingCache<URI, Collection<CertificateDto>> signingCertificates = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<URI, Collection<CertificateDto>>() {
                @Override
                public Collection<CertificateDto> load(URI key) {
                    return jsonClient.get(key, new GenericType<Collection<CertificateDto>>() {});
                }
            });
    private LoadingCache<URI, Collection<MatchingServiceConfigEntityDataDto>> msaConfigurations = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<URI, Collection<MatchingServiceConfigEntityDataDto>>() {
                @Override
                public Collection<MatchingServiceConfigEntityDataDto> load(URI key) {
                    return jsonClient.get(key, new GenericType<Collection<MatchingServiceConfigEntityDataDto>>() {});
                }
            });
    private LoadingCache<URI, Boolean> rpMetadataConfigurations = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<URI, Boolean>() {
                @Override
                public Boolean load(URI key) {
                    return jsonClient.get(key, Boolean.class);
                }
            });

    @Inject
    public ConfigProxy(
            JsonClient jsonClient,
            @Config URI configUri) {

        this.jsonClient = jsonClient;
        this.configUri = configUri;
    }

    @Timed
    public CertificateDto getEncryptionCertificate(String entityId) {
        URI uri = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId));
        try {
            return encryptionCertificates.getUnchecked(uri);
        } catch (UncheckedExecutionException e){
            Throwables.throwIfUnchecked(e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }

    @Timed
    public Collection<CertificateDto> getSignatureVerificationCertificates(String entityId) {
        URI uri = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId));
        try {
            return signingCertificates.getUnchecked(uri);
        } catch (UncheckedExecutionException e){
            Throwables.throwIfUnchecked(e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }

    @Timed
    public Optional<MatchingServiceConfigEntityDataDto> getMsaConfiguration(String entityId) {
        URI uri = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.MATCHING_SERVICE_ROOT)
                .build();
        try {
            final Collection<MatchingServiceConfigEntityDataDto> dtos = msaConfigurations.getUnchecked(uri);
            return dtos.stream().filter(e -> entityId.equals(e.getEntityId())).findFirst();
        } catch (UncheckedExecutionException e){
            Throwables.throwIfUnchecked(e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }

    @Timed
    public boolean getRPMetadataEnabled(String entityId) {
        URI uri = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.METADATA_LOCATION_RESOURCE)
                .build(entityId);
        try {
            return rpMetadataConfigurations.getUnchecked(uri);
        } catch (UncheckedExecutionException e){
            Throwables.throwIfUnchecked(e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }
}
