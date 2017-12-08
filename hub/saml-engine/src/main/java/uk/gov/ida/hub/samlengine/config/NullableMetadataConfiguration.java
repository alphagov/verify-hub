package uk.gov.ida.hub.samlengine.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import uk.gov.ida.saml.metadata.KeyStoreLoader;
import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;

import javax.validation.Valid;
import java.net.URI;
import java.security.KeyStore;

public class NullableMetadataConfiguration implements MetadataResolverConfiguration {

    private NullableMetadataConfiguration() {
    }

    public NullableMetadataConfiguration(String trustStorePath, String trustStorePassword, URI uri, Long minRefreshDelay, Long maxRefreshDelay, String expectedEntityId, JerseyClientConfiguration client, String jerseyClientName) {
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
        this.uri = uri;
        this.minRefreshDelay = minRefreshDelay;
        this.maxRefreshDelay = maxRefreshDelay;
        this.expectedEntityId = expectedEntityId;
        this.client = client;
        this.jerseyClientName = jerseyClientName;
    }

    @Valid
    @JsonProperty
    private String trustStorePath;

    @Valid
    @JsonProperty
    private String trustStorePassword;

    /* HTTP{S} URL the SAML metadata can be loaded from */
    @Valid
    @JsonProperty
    private URI uri;

    /* Used to set {@link org.opensaml.saml2.metadata.provider.AbstractReloadingMetadataProvider#minRefreshDelay} */
    @Valid
    @JsonProperty
    private Long minRefreshDelay;

    /* Used to set {@link org.opensaml.saml2.metadata.provider.AbstractReloadingMetadataProvider#maxRefreshDelay} */
    @Valid
    @JsonProperty
    private Long maxRefreshDelay;

    /*
    * What entityId can be expected to reliably appear in the SAML metadata?
    * Used to provide a healthcheck {@link uk.gov.ida.saml.dropwizard.metadata.MetadataHealthCheck}
    */
    @Valid
    @JsonProperty
    private String expectedEntityId;

    @Valid
    @JsonProperty
    private JerseyClientConfiguration client;

    @Valid
    @JsonProperty
    private String jerseyClientName = "MetadataClient";

    @Override
    public KeyStore getTrustStore() {
        return new KeyStoreLoader().load(trustStorePath, trustStorePassword);
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public Long getMinRefreshDelay() {
        return minRefreshDelay;
    }

    @Override
    public Long getMaxRefreshDelay() {
        return maxRefreshDelay;
    }

    @Override
    public String getExpectedEntityId() {
        return expectedEntityId;
    }

    @Override
    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return client;
    }

    @Override
    public String getJerseyClientName() {
        return jerseyClientName;
    }

    @Override
    public String getHubFederationId() {
        return "VERIFY-FEDERATION";
    }

}
