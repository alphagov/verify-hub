package uk.gov.ida.saml.hub.transformers.inbound;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromTransaction;

import java.net.URI;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class AuthnRequestFromTransactionUnmarshaller {
    public AuthnRequestFromTransaction fromSamlMessage(AuthnRequest authnRequest) {
        final String id = authnRequest.getID();
        final String issuerId = authnRequest.getIssuer().getValue();
        final DateTime issueInstant = authnRequest.getIssueInstant();
        final Boolean forceAuthn = authnRequest.isForceAuthn();
        final Integer assertionConsumerServiceIndex = authnRequest.getAssertionConsumerServiceIndex();
        final Optional<String> assertionConsumerServiceURL = Optional.ofNullable(authnRequest.getAssertionConsumerServiceURL());
        final Signature signature = authnRequest.getSignature();

        return AuthnRequestFromTransaction.createRequestReceivedFromTransaction(
                id,
                issuerId,
                issueInstant,
                forceAuthn,
                assertionConsumerServiceURL.map(URI::create),
                ofNullable(assertionConsumerServiceIndex),
                ofNullable(signature),
                URI.create(authnRequest.getDestination()));
    }
}
