package uk.gov.ida.hub.samlengine.contracts;

import java.net.URI;
import java.util.Optional;

public class TranslatedAuthnRequestDto {
    private String id;
    private String issuer;
    private Optional<Boolean> forceAuthentication;
    private Optional<URI> assertionConsumerServiceUrl;
    private Optional<Integer> assertionConsumerServiceIndex;

    @SuppressWarnings("unused") // needed for JAXB
    private TranslatedAuthnRequestDto() {
    }

    public TranslatedAuthnRequestDto(
            String id,
            String issuer,
            Optional<Boolean> forceAuthentication,
            Optional<URI> assertionConsumerServiceUrl,
            Optional<Integer> assertionConsumerServiceIndex) {
        this.id = id;
        this.issuer = issuer;
        this.forceAuthentication = forceAuthentication;
        this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
        this.assertionConsumerServiceIndex = assertionConsumerServiceIndex;
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

    public Optional<Integer> getAssertionConsumerServiceIndex() {
        return assertionConsumerServiceIndex;
    }

    public Optional<URI> getAssertionConsumerServiceUrl() {
        return assertionConsumerServiceUrl;
    }
}
