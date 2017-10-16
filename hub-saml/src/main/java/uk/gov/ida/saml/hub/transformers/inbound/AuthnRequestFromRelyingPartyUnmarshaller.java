package uk.gov.ida.saml.hub.transformers.inbound;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.EncryptedAttribute;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.xmlsec.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.saml.core.extensions.versioning.Version;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromRelyingParty;

import java.net.URI;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class AuthnRequestFromRelyingPartyUnmarshaller {

    private static final Logger LOG = LoggerFactory.getLogger(AuthnRequestFromRelyingPartyUnmarshaller.class);

    private final Decrypter decrypter;

    public AuthnRequestFromRelyingPartyUnmarshaller(Decrypter decrypter) {
        this.decrypter = decrypter;
    }

    public AuthnRequestFromRelyingParty fromSamlMessage(AuthnRequest authnRequest) {
        final String id = authnRequest.getID();
        final String issuerId = authnRequest.getIssuer().getValue();
        final DateTime issueInstant = authnRequest.getIssueInstant();
        final Boolean forceAuthn = authnRequest.isForceAuthn();
        final Optional<String> assertionConsumerServiceURL = Optional.ofNullable(authnRequest.getAssertionConsumerServiceURL());
        final Integer assertionConsumerServiceIndex = authnRequest.getAssertionConsumerServiceIndex();
        final Signature signature = authnRequest.getSignature();
        final Optional<String> verifyServiceProviderVersion = extractVerifyServiceProviderVersion(authnRequest.getExtensions());

        return new AuthnRequestFromRelyingParty(
            id,
            issuerId,
            issueInstant,
            URI.create(authnRequest.getDestination()),
            Optional.ofNullable(forceAuthn),
            assertionConsumerServiceURL.map(URI::create),
            ofNullable(assertionConsumerServiceIndex),
            ofNullable(signature),
            verifyServiceProviderVersion
        );
    }

    private Optional<String> extractVerifyServiceProviderVersion(Extensions extensions) {
        return Optional.ofNullable(extensions).flatMap(item -> {
            EncryptedAttribute encryptedAttribute = (EncryptedAttribute) extensions.getUnknownXMLObjects().get(0);
            try {
                Attribute attribute = decrypter.decrypt(encryptedAttribute);
                Version version = (Version) attribute.getAttributeValues().get(0);
                return Optional.of(version.getApplicationVersion().getValue());
            } catch (Exception e) {
                LOG.error("Error while decrypting VSP version", e);
                return Optional.empty();
            }
        });
    }
}
