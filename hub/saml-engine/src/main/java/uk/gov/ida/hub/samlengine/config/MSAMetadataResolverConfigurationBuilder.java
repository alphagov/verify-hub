package uk.gov.ida.hub.samlengine.config;

import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.security.KeyStore;

public class MSAMetadataResolverConfigurationBuilder {

    private KeyStore trustStore;
    private URI uri;
    private String expectedEntityId;
    private String hubFederationId = "VERIFY-FEDERATION"; // NOTE: this is not set by MSAs!

    private MSAMetadataResolverConfigurationBuilder() {}

    public static MSAMetadataResolverConfigurationBuilder aConfig() {
        return new MSAMetadataResolverConfigurationBuilder();
    }

    public MetadataResolverConfiguration build() {
        return new MSAMetadataResolverConfiguration(this.trustStore, this.uri, this.expectedEntityId, this.hubFederationId);
    }

    public MSAMetadataResolverConfigurationBuilder withMsaEntityId(String msaEntityId) {
        this.expectedEntityId = msaEntityId;
        return this;
    }

    public MSAMetadataResolverConfigurationBuilder withUri(String uri) {
        this.uri = UriBuilder.fromUri(uri).build();
        return this;
    }

    public MSAMetadataResolverConfigurationBuilder withTrustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
        return this;
    }

    public MSAMetadataResolverConfigurationBuilder withHubFederationId(String hubFederationId) {
        this.hubFederationId = hubFederationId;
        return this;
    }

}
