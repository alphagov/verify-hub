package uk.gov.ida.hub.policy.proxy;

import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class SamlResponseWithAuthnRequestInformationDtoBuilder {

    private String id = UUID.randomUUID().toString();
    private String issuer = UUID.randomUUID().toString();
    private Optional<Boolean> forceAuthentication = Optional.empty();
    private Optional<Integer> assertionConsumerServiceIndex = Optional.of(1);
    private Optional<URI> assertionConsumerServiceUrl = Optional.empty();


    public static SamlResponseWithAuthnRequestInformationDtoBuilder aSamlResponseWithAuthnRequestInformationDto() {
        return new SamlResponseWithAuthnRequestInformationDtoBuilder();
    }

    public SamlResponseWithAuthnRequestInformationDto build() {
        return new SamlResponseWithAuthnRequestInformationDto(id, issuer, forceAuthentication, assertionConsumerServiceIndex, assertionConsumerServiceUrl);
    }

    public SamlResponseWithAuthnRequestInformationDtoBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public SamlResponseWithAuthnRequestInformationDtoBuilder withIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public SamlResponseWithAuthnRequestInformationDtoBuilder withAssertionConsumerServiceUrl(URI uri) {
        this.assertionConsumerServiceUrl = Optional.ofNullable(uri);
        return this;
    }

}
