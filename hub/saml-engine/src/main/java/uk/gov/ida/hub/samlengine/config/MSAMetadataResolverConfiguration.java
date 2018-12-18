package uk.gov.ida.hub.samlengine.config;

import io.dropwizard.client.JerseyClientConfiguration;
import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;

import java.net.URI;
import java.security.KeyStore;

public class MSAMetadataResolverConfiguration implements MetadataResolverConfiguration {
    private KeyStore trustStore;
    private URI uri;
    private String expectedEntityId;
    private String hubFederationId;

    MSAMetadataResolverConfiguration(KeyStore trustStore, URI uri, String expectedEntityId, String hubFederationId) {
        this.trustStore = trustStore;
        this.uri = uri;
        this.expectedEntityId = expectedEntityId;
        this.hubFederationId = hubFederationId;
    }

    @Override
    public KeyStore getTrustStore() {
        return trustStore;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public Long getMinRefreshDelay() {
        return Long.valueOf(60000L);
    }

    @Override
    public Long getMaxRefreshDelay() {
        return Long.valueOf(600000L);
    }

    @Override
    public String getExpectedEntityId() {
        return expectedEntityId;
    }

    @Override
    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return new JerseyClientConfiguration();
    }

    @Override
    public String getJerseyClientName() {
        return "MSAMetdataClient-"+expectedEntityId;
    }

    @Override
    public String getHubFederationId() {
        return hubFederationId;
    }
}
