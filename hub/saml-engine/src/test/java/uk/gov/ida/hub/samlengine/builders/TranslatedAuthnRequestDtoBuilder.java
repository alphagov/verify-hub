package uk.gov.ida.hub.samlengine.builders;

import uk.gov.ida.hub.samlengine.contracts.TranslatedAuthnRequestDto;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class TranslatedAuthnRequestDtoBuilder {
    private String id = UUID.randomUUID().toString();
    private String issuer = UUID.randomUUID().toString();
    private Optional<Boolean> forceAuthentication = Optional.empty();
    private Optional<URI> assertionConsumerServiceUrl = Optional.empty();
    private Optional<Integer> assertionConsumerServiceIndex = Optional.empty();


    public static TranslatedAuthnRequestDtoBuilder aTranslatedAuthnRequest() {
        return new TranslatedAuthnRequestDtoBuilder();
    }

    public TranslatedAuthnRequestDto build() {
        return new TranslatedAuthnRequestDto(id, issuer, forceAuthentication, assertionConsumerServiceUrl, assertionConsumerServiceIndex);
    }

    public TranslatedAuthnRequestDtoBuilder withIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public TranslatedAuthnRequestDtoBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public TranslatedAuthnRequestDtoBuilder withForceAuthentication(boolean forceAuthentication) {
        this.forceAuthentication = Optional.of(forceAuthentication);
        return this;
    }

    public TranslatedAuthnRequestDtoBuilder withAssertionConsumerServiceUrl(URI assertionConsumerServiceUrl) {
        this.assertionConsumerServiceUrl = Optional.of(assertionConsumerServiceUrl);
        return this;
    }

    public TranslatedAuthnRequestDtoBuilder withAssertionConsumerServiceIndex(int assertionConsumerServiceIndex) {
        this.assertionConsumerServiceIndex = Optional.of(assertionConsumerServiceIndex);
        return this;
    }
}
