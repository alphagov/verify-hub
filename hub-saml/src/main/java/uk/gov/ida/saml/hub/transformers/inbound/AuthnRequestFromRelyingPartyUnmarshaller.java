package uk.gov.ida.saml.hub.transformers.inbound;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.EncryptedAttribute;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
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
        final Optional<String> verifyServiceProviderVersion = extractVerifyServiceProviderVersion(authnRequest.getExtensions(), issuerId);

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

    private Optional<String> extractVerifyServiceProviderVersion(Extensions extensions, String issuerId) {
        return Optional.ofNullable(extensions).flatMap(item -> {
            try {
                return extensions.getUnknownXMLObjects().stream()
                    .filter(EncryptedAttribute.class::isInstance)
                    .findFirst()
                    .map(EncryptedAttribute.class::cast)
                    .map(this::decrypt)
                    .map(this::extractVersion);
            } catch (Exception e) {
                LOG.error("Error while processing the VSP version for issuer " + issuerId, e);
                return Optional.empty();
            }
        });
    }

    private String extractVersion(Attribute attribute) {
        return attribute.getAttributeValues().stream()
            .filter(Version.class::isInstance)
            .findFirst()
            .map(Version.class::cast)
            .map(version -> version.getApplicationVersion().getValue())
            .orElseThrow(() -> new RuntimeException("Attribute does not contain VSP Version"));
    }

    private Attribute decrypt(EncryptedAttribute encryptedAttribute) {
        try {
            return decrypter.decrypt(encryptedAttribute);
        } catch (DecryptionException e) {
            throw new RuntimeException(e);
        }
    }
}
