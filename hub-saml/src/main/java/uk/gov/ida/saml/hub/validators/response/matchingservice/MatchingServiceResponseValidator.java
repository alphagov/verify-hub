package uk.gov.ida.saml.hub.validators.response.matchingservice;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.exception.SamlFailedToDecryptException;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import java.util.List;

public class MatchingServiceResponseValidator {
    private static final Logger log = LoggerFactory.getLogger(MatchingServiceResponseValidator.class.getSimpleName());

    private final EncryptedResponseFromMatchingServiceValidator responseFromMatchingServiceValidator;
    private final SamlResponseSignatureValidator samlResponseSignatureValidator;
    private final List<AssertionDecrypter> assertionDecrypters;
    private final SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    private final ResponseAssertionsFromMatchingServiceValidator responseAssertionsFromMatchingServiceValidator;
    private ValidatedResponse validatedResponse;
    private ValidatedAssertions validatedAssertions;

    public MatchingServiceResponseValidator(
        EncryptedResponseFromMatchingServiceValidator responseFromMatchingServiceValidator,
        SamlResponseSignatureValidator samlResponseSignatureValidator,
        List<AssertionDecrypter> assertionDecrypters,
        SamlAssertionsSignatureValidator samlAssertionsSignatureValidator,
        ResponseAssertionsFromMatchingServiceValidator responseAssertionsFromMatchingServiceValidator) {
        this.responseFromMatchingServiceValidator = responseFromMatchingServiceValidator;
        this.samlResponseSignatureValidator = samlResponseSignatureValidator;
        this.assertionDecrypters = assertionDecrypters;
        this.samlAssertionsSignatureValidator = samlAssertionsSignatureValidator;
        this.responseAssertionsFromMatchingServiceValidator = responseAssertionsFromMatchingServiceValidator;
    }

    public ValidatedResponse getValidatedResponse() {
        return validatedResponse;
    }

    public ValidatedAssertions getValidatedAssertions() {
        return validatedAssertions;
    }

    public void validate(Response response) {
        responseFromMatchingServiceValidator.validate(response);

        validatedResponse = samlResponseSignatureValidator.validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);

        /* Decrypt and validate assertions independently. */
        List<Assertion> decryptedAssertions = null;

        for (AssertionDecrypter assertionDecrypter : assertionDecrypters) {
            try {
                decryptedAssertions = assertionDecrypter.decryptAssertions(validatedResponse);
            } catch(SamlFailedToDecryptException e) {
                log.warn("Failed to decrypt MSA assertions with one of the decrypters", e);
            }
        }

        if (decryptedAssertions == null) {
            throw new SamlFailedToDecryptException("Could not decrypt MSA assertions with any of the decrypters", Level.ERROR);
        }

        validatedAssertions = samlAssertionsSignatureValidator.validate(decryptedAssertions, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);

        responseAssertionsFromMatchingServiceValidator.validate(validatedResponse, validatedAssertions);
    }
}
