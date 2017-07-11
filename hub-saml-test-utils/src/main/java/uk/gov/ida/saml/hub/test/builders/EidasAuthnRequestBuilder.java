package uk.gov.ida.saml.hub.test.builders;

import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public class EidasAuthnRequestBuilder {
    private String id = UUID.randomUUID().toString();
    private String issuer = "issuer_id";
    private DateTime issueInstant = DateTime.now();
    private String destination;
    private String providerName;
    private List<AuthnContext> authnContextList;

    public static EidasAuthnRequestBuilder anEidasAuthnRequest() {
        return new EidasAuthnRequestBuilder();
    }

    public EidasAuthnRequestFromHub buildFromHub() {
        return new EidasAuthnRequestFromHub(
            id,
            issuer,
            issueInstant,
            authnContextList,
            URI.create("http://eidas/ssoLocation"),
            providerName);
    }

    public EidasAuthnRequestBuilder withLevelsOfAssurance(List<AuthnContext> authnContextList) {
        this.authnContextList = authnContextList;
        return this;
    }

    public EidasAuthnRequestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public EidasAuthnRequestBuilder withDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public EidasAuthnRequestBuilder withProviderName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    public EidasAuthnRequestBuilder withIssuer(String issuerEntityId) {
        this.issuer = issuerEntityId;
        return this;
    }
}
