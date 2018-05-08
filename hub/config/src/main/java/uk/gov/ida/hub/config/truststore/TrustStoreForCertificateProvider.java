package uk.gov.ida.hub.config.truststore;

import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.KeyStoreCache;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.inject.Inject;
import java.security.KeyStore;

import static java.text.MessageFormat.format;

public class TrustStoreForCertificateProvider {

    private final TrustStoreConfiguration trustStoreConfiguration;
    private final KeyStoreCache keyStoreCache;
    private final ConfigConfiguration configConfiguration;

    @Inject
    public TrustStoreForCertificateProvider(
        final TrustStoreConfiguration trustStoreConfiguration,
        final KeyStoreCache keyStoreCache,
        final ConfigConfiguration configConfiguration) {
        this.trustStoreConfiguration = trustStoreConfiguration;
        this.keyStoreCache = keyStoreCache;
        this.configConfiguration = configConfiguration;
    }

    public KeyStore getTrustStoreFor(FederationEntityType federationEntityType) {
        ClientTrustStoreConfiguration configuration = getAppropriateTrustStoreConfig(federationEntityType);
        return keyStoreCache.get(configuration);
    }

    private ClientTrustStoreConfiguration getAppropriateTrustStoreConfig(final FederationEntityType federationEntityType) {
        switch (federationEntityType) {
            case HUB:
            case IDP:
                return configConfiguration.getClientTrustStoreConfiguration();
            case RP:
            case MS:
                return trustStoreConfiguration.getRpTrustStoreConfiguration();
            default:
                throw new IllegalArgumentException(format("Unexpected federation entity type: {0}", federationEntityType));
        }
    }
}
