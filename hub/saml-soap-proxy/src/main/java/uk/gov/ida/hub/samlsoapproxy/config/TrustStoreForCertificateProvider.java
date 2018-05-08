package uk.gov.ida.hub.samlsoapproxy.config;

import uk.gov.ida.hub.samlsoapproxy.domain.FederationEntityType;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.KeyStoreCache;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.inject.Inject;
import java.security.KeyStore;

import static java.text.MessageFormat.format;

public class TrustStoreForCertificateProvider {

    private TrustStoreConfiguration trustStoreConfiguration;
    private final KeyStoreCache keyStoreCache;

    @Inject
    public TrustStoreForCertificateProvider(final TrustStoreConfiguration trustStoreConfiguration, KeyStoreCache keyStoreCache) {
        this.trustStoreConfiguration = trustStoreConfiguration;
        this.keyStoreCache = keyStoreCache;
    }

    public KeyStore getTrustStoreFor(FederationEntityType federationEntityType) {
        ClientTrustStoreConfiguration configuration = getAppropriateTrustStoreConfig(federationEntityType);
        return keyStoreCache.get(configuration);
    }

    private ClientTrustStoreConfiguration getAppropriateTrustStoreConfig(final FederationEntityType federationEntityType) {
        switch (federationEntityType) {
            case RP:
            case MS:
                return trustStoreConfiguration.getRpTrustStoreConfiguration();
            default:
                throw new IllegalArgumentException(format("Unexpected federation entity type: {0}", federationEntityType));
        }
    }
}
