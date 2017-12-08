package uk.gov.ida.hub.samlengine.builders;

import org.joda.time.DateTime;
import uk.gov.ida.hub.samlengine.contracts.SamlRequestWithAuthnRequestInformationDto;
import uk.gov.ida.saml.core.test.AuthnRequestFactory;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.hub.domain.Endpoints;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import java.net.URI;
import java.util.Optional;

import static uk.gov.ida.saml.core.test.AuthnRequestIdGenerator.generateRequestId;

public class SamlAuthnRequestDtoBuilder {

    private final AuthnRequestFactory authnRequestFactory;
    private String issuer = TestEntityIds.TEST_RP;
    private String id = generateRequestId();
    private Optional<Boolean> forceAuthentication = Optional.empty();
    private Optional<Integer> assertionConsumerServiceIndex = Optional.empty();
    private Optional<URI> assertionConsumerServiceUrl = Optional.empty();
    private Optional<DateTime> issueInstant = Optional.empty();
    private String publicCert;
    private String privateKey;

    public static SamlAuthnRequestDtoBuilder aSamlAuthnRequest() {
        return new SamlAuthnRequestDtoBuilder();
    }

    private SamlAuthnRequestDtoBuilder() {
        this.authnRequestFactory = new AuthnRequestFactory(new XmlObjectToBase64EncodedStringTransformer<>());
    }

    public SamlRequestWithAuthnRequestInformationDto build() {
        String samlMessage = authnRequestFactory.anAuthnRequest(id, issuer, forceAuthentication, assertionConsumerServiceUrl, assertionConsumerServiceIndex, publicCert, privateKey, Endpoints.SSO_REQUEST_ENDPOINT, issueInstant);
        return new SamlRequestWithAuthnRequestInformationDto(samlMessage);
    }

    public SamlRequestWithAuthnRequestInformationDto buildInvalid() {
        String samlMessage = authnRequestFactory.anInvalidAuthnRequest(id, issuer, forceAuthentication, assertionConsumerServiceUrl, assertionConsumerServiceIndex, publicCert, privateKey, Endpoints.SSO_REQUEST_ENDPOINT, issueInstant);
        return new SamlRequestWithAuthnRequestInformationDto(samlMessage);
    }

    public SamlAuthnRequestDtoBuilder withIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public SamlAuthnRequestDtoBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public SamlAuthnRequestDtoBuilder withForceAuthentication(boolean forceAuthentication) {
        this.forceAuthentication = Optional.of(forceAuthentication);
        return this;
    }

    public SamlAuthnRequestDtoBuilder withAssertionConsumerUrl(URI assertionConsumerServiceUrl) {
        this.assertionConsumerServiceUrl = Optional.of(assertionConsumerServiceUrl);
        return this;
    }

    public SamlAuthnRequestDtoBuilder withAssertionConsumerIndex(int assertionConsumerServiceIndex) {
        this.assertionConsumerServiceIndex = Optional.of(assertionConsumerServiceIndex);
        return this;
    }

    public SamlAuthnRequestDtoBuilder withIssueInstant(DateTime issueInstant) {
        this.issueInstant = Optional.of(issueInstant);
        return this;
    }

    public SamlAuthnRequestDtoBuilder withPublicCert(String publicCert) {
        this.publicCert = publicCert;
        return this;
    }

    public SamlAuthnRequestDtoBuilder withPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }
}
