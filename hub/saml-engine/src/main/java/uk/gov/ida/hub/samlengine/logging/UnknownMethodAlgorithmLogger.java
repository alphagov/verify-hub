package uk.gov.ida.hub.samlengine.logging;

import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.SignedInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA512;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureImpl;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromRelyingParty;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;

import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * UnknownMethodAlgorithmLogger is responsible for logging identity providers and service providers
 * using unknown signature and digest method algorithms.
 */
public final class UnknownMethodAlgorithmLogger {

    public static final String SIGNATURE_AND_DIGEST_ALGORITHMS_MESSAGE =
            "%s is using %s and %s for signature and digest method algorithms in %s respectively.";
    public static final String SIGNATURE_ALGORITHM_MESSAGE =
            "%s is using %s for signature method algorithm in %s.";
    public static final String DIGEST_ALGORITHM_MESSAGE =
            "%s is using %s for digest method algorithm in %s.";

    private static final Logger LOG = LoggerFactory.getLogger(UnknownMethodAlgorithmLogger.class);
    private static final Set<String> VALID_SIGNATURE_ALGORITHMS = Set.of(
            SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256,
            SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512
    );
    private static final Set<String> VALID_DIGEST_ALGORITHMS = Set.of(
            new DigestSHA256(),
            new DigestSHA512()
    ).stream().map(DigestAlgorithm::getJCAAlgorithmID).collect(Collectors.toUnmodifiableSet());

    private static String getDigestMethodAlgorithm(final Signature signature) {
        XMLSignature xmlSignature = ((SignatureImpl) signature).getXMLSignature();
        if (xmlSignature != null) {
            SignedInfo signedInfo = xmlSignature.getSignedInfo();
            try {
                if (signedInfo != null && signedInfo.getLength() != 0 && signedInfo.item(0) != null) {
                    MessageDigestAlgorithm messageDigestAlgorithm = signedInfo.item(0).getMessageDigestAlgorithm();
                    if (messageDigestAlgorithm != null) {
                        return messageDigestAlgorithm.getJCEAlgorithmString();
                    }
                }
            } catch (XMLSecurityException e) {
                LOG.debug("Error getting message digest algorithm", e);
            }
        }
        return null;
    }

    private static void logMethodAlgorithm(
            final Role role,
            final Signature signature,
            final String location) {
        final String signatureMethodAlgorithm = signature.getSignatureAlgorithm();
        final String digestMethodAlgorithm = getDigestMethodAlgorithm(signature);
        final boolean validSignatureAlgorithm = VALID_SIGNATURE_ALGORITHMS.contains(signatureMethodAlgorithm);
        final boolean validDigestAlgorithm = VALID_DIGEST_ALGORITHMS.contains(digestMethodAlgorithm);
        if (!validSignatureAlgorithm && !validDigestAlgorithm) {
            LOG.info(format(SIGNATURE_AND_DIGEST_ALGORITHMS_MESSAGE, role, signatureMethodAlgorithm, digestMethodAlgorithm, location));
        } else if (!validSignatureAlgorithm) {
            LOG.info(format(SIGNATURE_ALGORITHM_MESSAGE, role, signatureMethodAlgorithm, location));
        } else if (!validDigestAlgorithm) {
            LOG.info(format(DIGEST_ALGORITHM_MESSAGE, role, digestMethodAlgorithm, location));
        }
    }

    public static void probeResponseForMethodAlgorithm(final InboundResponseFromIdp response) {
        if (response != null) {
            response.getSignature().ifPresent((signature) ->
                logMethodAlgorithm(Role.IDP, signature, Response.DEFAULT_ELEMENT_LOCAL_NAME)
            );
        }
    }

    public static void probeAssertionForMethodAlgorithm(final Assertion assertion, final String typeOfAssertion) {
        String prefixAssertion = typeOfAssertion + Assertion.DEFAULT_ELEMENT_LOCAL_NAME;
        if (assertion != null) {
            Signature signature = assertion.getSignature();
            if (signature != null) {
                logMethodAlgorithm(Role.IDP, signature, prefixAssertion);
            }
        }
    }

    public static void probeAuthnRequestForMethodAlgorithm(final AuthnRequestFromRelyingParty authnRequest) {
        if (authnRequest != null) {
            authnRequest.getSignature().ifPresent((signature) ->
                logMethodAlgorithm(Role.SP, signature, AuthnRequest.DEFAULT_ELEMENT_LOCAL_NAME)
            );
        }
    }
}
