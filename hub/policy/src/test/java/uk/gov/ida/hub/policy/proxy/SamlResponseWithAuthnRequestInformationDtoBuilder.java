package uk.gov.ida.hub.policy.proxy;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;

import java.net.URI;
import java.util.UUID;

import static com.google.common.base.Optional.absent;

public class SamlResponseWithAuthnRequestInformationDtoBuilder {

    private String id = UUID.randomUUID().toString();
    private String issuer = UUID.randomUUID().toString();
    private Optional<Boolean> forceAuthentication = absent();
    private Optional<Integer> assertionConsumerServiceIndex = Optional.of(1);
    private Optional<URI> assertionConsumerServiceUrl = Optional.absent();


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
        this.assertionConsumerServiceUrl = Optional.fromNullable(uri);
        return this;
    }

}
