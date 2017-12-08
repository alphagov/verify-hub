package uk.gov.ida.hub.samlengine.builders;

import org.joda.time.DateTime;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromRelyingParty;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static java.lang.Boolean.FALSE;

public class AuthnRequestFromRelyingPartyBuilder {

    private String id = UUID.randomUUID().toString();
    private String issuer = UUID.randomUUID().toString();
    private DateTime issueInstant = DateTime.now();
    private URI destination = UriBuilder.fromUri(UUID.randomUUID().toString()).build();
    private Boolean forceAuthentication = FALSE;
    private URI assertionConsumerServiceUrl = URI.create("http://example.com");
    private Integer assertionConsumerServiceIndex = null;
    private Signature signature = null;
    private String verifyServiceProviderVersion = "default-service-provider-version";

    public static AuthnRequestFromRelyingPartyBuilder anAuthnRequestFromRelyingParty() {
        return new AuthnRequestFromRelyingPartyBuilder();
    }

    public static AuthnRequestFromRelyingParty anyAuthnRequestFromRelyingParty() {
        return anAuthnRequestFromRelyingParty().build();
    }

    public AuthnRequestFromRelyingParty build() {
        return new AuthnRequestFromRelyingParty(
            id,
            issuer,
            issueInstant,
            destination,
            Optional.ofNullable(forceAuthentication),
            Optional.ofNullable(assertionConsumerServiceUrl),
            Optional.ofNullable(assertionConsumerServiceIndex),
            Optional.ofNullable(signature),
            Optional.ofNullable(verifyServiceProviderVersion)
        );
    }

    public AuthnRequestFromRelyingPartyBuilder withId(final String id) {
        this.id = id;
        return this;
    }

    public AuthnRequestFromRelyingPartyBuilder withIssuer(final String issuer) {
        this.issuer = issuer;
        return this;
    }

    public AuthnRequestFromRelyingPartyBuilder withIssueInstant(final DateTime issueInstant) {
        this.issueInstant = issueInstant;
        return this;
    }

    public AuthnRequestFromRelyingPartyBuilder withDestination(final URI destination) {
        this.destination = destination;
        return this;
    }

    public AuthnRequestFromRelyingPartyBuilder withForceAuthentication(Boolean forceAuthentication) {
        this.forceAuthentication = forceAuthentication;
        return this;
    }

    public AuthnRequestFromRelyingPartyBuilder withAssertionConsumerServiceUrl(URI assertionConsumerServiceUrl) {
        this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
        return this;
    }

    public AuthnRequestFromRelyingPartyBuilder withAssertionConsumerServiceIndex(Integer assertionConsumerServiceIndex) {
        this.assertionConsumerServiceIndex = assertionConsumerServiceIndex;
        return this;
    }

    public AuthnRequestFromRelyingPartyBuilder withSignature(Signature signature) {
        this.signature = signature;
        return this;
    }

    public AuthnRequestFromRelyingPartyBuilder withVerifyServiceProviderVersion(String verifyServiceProviderVersion) {
        this.verifyServiceProviderVersion = verifyServiceProviderVersion;
        return this;
    }
}
