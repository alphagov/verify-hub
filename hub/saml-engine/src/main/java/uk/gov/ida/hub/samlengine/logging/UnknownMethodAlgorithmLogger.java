package uk.gov.ida.hub.samlengine.logging;

import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.SignedInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA512;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureImpl;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromRelyingParty;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromTransaction;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.text.MessageFormat.format;

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
    private static final Set<String> VALID_SIGNATURE_ALGORITHMS = new HashSet<>();
    private static final Set<String> VALID_DIGEST_ALGORITHMS = new HashSet<>();

    static {
        VALID_SIGNATURE_ALGORITHMS.add(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        VALID_SIGNATURE_ALGORITHMS.add(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512);
        VALID_DIGEST_ALGORITHMS.add(new DigestSHA256().getJCAAlgorithmID());
        VALID_DIGEST_ALGORITHMS.add(new DigestSHA512().getJCAAlgorithmID());
    }

    private static String getSignatureMethodAlgorithm(final Optional<Signature> signature) {
        if (signature.isPresent()) {
            return signature.get().getSignatureAlgorithm();
        }
        return null;
    }

    private static String getDigestMethodAlgorithm(final Optional<Signature> signature) {
        if (signature.isPresent()) {
            XMLSignature xmlSignature = ((SignatureImpl) signature.get()).getXMLSignature();
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
                    LOG.debug(format("Error getting message digest algorithm: {0}", e));
                }
            }
        }
        return null;
    }

    private static void logMethodAlgorithm(
            final Role role,
            final String signatureMethodAlgorithm,
            final String digestMethodAlgorithm,
            final String location) {
        final boolean validSignatureAlgorithm = VALID_SIGNATURE_ALGORITHMS.contains(signatureMethodAlgorithm);
        final boolean validDigestAlgorithm = VALID_DIGEST_ALGORITHMS.contains(digestMethodAlgorithm);
        if (!validSignatureAlgorithm && !validDigestAlgorithm) {
            LOG.info(
                String.format(SIGNATURE_AND_DIGEST_ALGORITHMS_MESSAGE, role, signatureMethodAlgorithm, digestMethodAlgorithm, location)
            );
        }
        else if (!validSignatureAlgorithm) {
            LOG.info(
                    String.format(SIGNATURE_ALGORITHM_MESSAGE, role, signatureMethodAlgorithm, location)
            );
        }
        else if (!validDigestAlgorithm) {
            LOG.info(
                    String.format(DIGEST_ALGORITHM_MESSAGE, role, digestMethodAlgorithm, location)
            );
        }
    }

    public static void probeResponseForMethodAlgorithm(final InboundResponseFromIdp response) {
        if (response != null) {
            final Optional<Signature> signature = response.getSignature();
            if (signature != null) {
                final String signatureMethodAlgorithm = getSignatureMethodAlgorithm(signature);
                final String digestMethodAlgorithm = getDigestMethodAlgorithm(signature);
                logMethodAlgorithm(Role.IDP, signatureMethodAlgorithm, digestMethodAlgorithm, Response.DEFAULT_ELEMENT_LOCAL_NAME);
            }
        }
    }

    public static void probeAssertionForMethodAlgorithm(final Assertion assertion, final String typeOfAssertion) {
        String prefixAssertion = typeOfAssertion + Assertion.DEFAULT_ELEMENT_LOCAL_NAME;
        if (assertion != null) {
            final Optional<Signature> signature = Optional.ofNullable(assertion.getSignature());
            if (signature != null) {
                final String signatureMethodAlgorithm = getSignatureMethodAlgorithm(signature);
                final String digestMethodAlgorithm = getDigestMethodAlgorithm(signature);
                logMethodAlgorithm(Role.IDP, signatureMethodAlgorithm, digestMethodAlgorithm, prefixAssertion);
            }
        }
    }

    public static void probeAuthnRequestForMethodAlgorithm(final AuthnRequestFromRelyingParty authnRequest) {
        if (authnRequest != null) {
            final Optional<Signature> signature = authnRequest.getSignature();
            if (signature != null) {
                final String signatureMethodAlgorithm = getSignatureMethodAlgorithm(signature);
                final String digestMethodAlgorithm = getDigestMethodAlgorithm(signature);
                logMethodAlgorithm(Role.SP, signatureMethodAlgorithm, digestMethodAlgorithm, AuthnRequest.DEFAULT_ELEMENT_LOCAL_NAME);
            }
        }
    }
}
