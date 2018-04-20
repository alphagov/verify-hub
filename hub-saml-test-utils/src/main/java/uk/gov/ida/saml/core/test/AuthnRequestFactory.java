package uk.gov.ida.saml.core.test;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.AuthnRequest;
import uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder;
import uk.gov.ida.saml.core.test.builders.IssuerBuilder;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

public class AuthnRequestFactory {
    private final Function<AuthnRequest, String> authnRequestToStringTransformer;

    public AuthnRequestFactory(Function<AuthnRequest, String> authnRequestToStringTransformer) {
        this.authnRequestToStringTransformer = authnRequestToStringTransformer;
    }

    public String anAuthnRequest(
            String id,
            String issuer,
            Optional<Boolean> forceAuthentication,
            Optional<URI> assertionConsumerServiceUrl,
            Optional<Integer> assertionConsumerServiceIndex,
            String publicCert,
            String privateKey,
            String ssoRequestEndpoint,
            Optional<DateTime> issueInstant) {
        AuthnRequest authnRequest = getAuthnRequest(
                id,
                issuer,
                forceAuthentication,
                assertionConsumerServiceUrl,
                assertionConsumerServiceIndex,
                publicCert,
                privateKey,
                ssoRequestEndpoint,
                issueInstant);
        return authnRequestToStringTransformer.apply(authnRequest);
    }

    private AuthnRequest getAuthnRequest(
            String id,
            String issuer,
            Optional<Boolean> forceAuthentication,
            Optional<URI> assertionConsumerServiceUrl,
            Optional<Integer> assertionConsumerServiceIndex,
            String publicCert,
            String privateKey,
            String ssoRequestEndpoint,
            Optional<DateTime> issueInstant) {
        AuthnRequestBuilder authnRequestBuilder = AuthnRequestBuilder.anAuthnRequest()
                .withId(id)
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(issuer).build())
                .withDestination("http://localhost" + ssoRequestEndpoint)
                .withSigningCredential(new TestCredentialFactory(publicCert, privateKey).getSigningCredential());

        forceAuthentication.ifPresent(authnRequestBuilder::withForceAuthn);
        assertionConsumerServiceIndex.ifPresent(authnRequestBuilder::withAssertionConsumerServiceIndex);
        assertionConsumerServiceUrl.ifPresent(uri -> authnRequestBuilder.withAssertionConsumerServiceUrl(uri.toString()));
        issueInstant.ifPresent(authnRequestBuilder::withIssueInstant);

        return authnRequestBuilder.build();
    }

    public String anInvalidAuthnRequest(
            String id,
            String issuer,
            Optional<Boolean> forceAuthentication,
            Optional<URI> assertionConsumerServiceUrl,
            Optional<Integer> assertionConsumerServiceIndex,
            String publicCert,
            String privateKey,
            String ssoRequestEndpoint,
            Optional<DateTime> issueInstant) {
        // Pad ID to ensure request is long enough
        AuthnRequest authnRequest = getAuthnRequest(
                StringUtils.rightPad(id, 1200, "x"),
                issuer,
                forceAuthentication,
                assertionConsumerServiceUrl,
                assertionConsumerServiceIndex,
                publicCert,
                privateKey,
                ssoRequestEndpoint,
                issueInstant);
        authnRequest.setSignature(null);
        // Use a different transformer to ensure that no Signature elements are added
        XmlObjectToBase64EncodedStringTransformer<XMLObject> transformer = new XmlObjectToBase64EncodedStringTransformer<>();
        return transformer.apply(authnRequest);
    }
}
