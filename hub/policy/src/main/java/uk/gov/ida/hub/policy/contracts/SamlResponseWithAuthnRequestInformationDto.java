package uk.gov.ida.hub.policy.contracts;

import java.net.URI;
import java.util.Optional;

public class SamlResponseWithAuthnRequestInformationDto {

    private String id;
    private String issuer;
    private Optional<Boolean> forceAuthentication;
    private Optional<URI> assertionConsumerServiceUrl;
    private Optional<Integer> assertionConsumerServiceIndex;

    @SuppressWarnings("unused") // needed for JAXB
    private SamlResponseWithAuthnRequestInformationDto() {
    }

    public SamlResponseWithAuthnRequestInformationDto(
            final String id,
            final String issuer,
            final Optional<Boolean> forceAuthentication,
            final Optional<Integer> assertionConsumerServiceIndex,
            final Optional<URI> assertionConsumerServiceUrl
    ) {
        this.id = id;
        this.issuer = issuer;
        this.forceAuthentication = forceAuthentication;
        this.assertionConsumerServiceIndex = assertionConsumerServiceIndex;
        this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
    }

    public String getId() {
        return id;
    }

    public String getIssuer() {
        return issuer;
    }

    public Optional<Boolean> getForceAuthentication() {
        return forceAuthentication;
    }

    public Optional<URI> getAssertionConsumerServiceUrl() {
        return assertionConsumerServiceUrl;
    }

    public Optional<Integer> getAssertionConsumerServiceIndex() {
        return assertionConsumerServiceIndex;
    }
}
