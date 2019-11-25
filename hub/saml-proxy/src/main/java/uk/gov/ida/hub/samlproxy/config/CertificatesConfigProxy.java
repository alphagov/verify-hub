package uk.gov.ida.hub.samlproxy.config;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Singleton;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.annotations.Config;
import uk.gov.ida.hub.samlproxy.domain.CertificateDto;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Singleton
public class CertificatesConfigProxy {

    private final JsonClient jsonClient;
    private final URI configUri;

    private LoadingCache<URI, CertificateDto> encryptionCertificates;
    private LoadingCache<URI, Collection<CertificateDto>> signingCertificates;

    @Inject
    public CertificatesConfigProxy(
            JsonClient jsonClient,
            @Config URI configUri,
            @Config long certificatesConfigCacheExpiryInSeconds) {

        this.jsonClient = jsonClient;
        this.configUri = configUri;

        encryptionCertificates = CacheBuilder.newBuilder()
                .expireAfterWrite(certificatesConfigCacheExpiryInSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public CertificateDto load(URI key) {
                        return jsonClient.get(key, CertificateDto.class);
                    }
                });

        signingCertificates = CacheBuilder.newBuilder()
                .expireAfterWrite(certificatesConfigCacheExpiryInSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public Collection<CertificateDto> load(URI key) {
                        return jsonClient.get(key, new GenericType<Collection<CertificateDto>>() {
                        });
                    }
                });
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
}
