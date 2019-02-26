package uk.gov.ida.saml.hub.validators.response.idp;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.EncryptedResponseFromIdpValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.ResponseAssertionsFromIdpValidator;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.exception.SamlFailedToDecryptException;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import java.util.List;

public class IdpResponseValidator {
    private static final Logger log = LoggerFactory.getLogger(IdpResponseValidator.class.getSimpleName());

    private final SamlResponseSignatureValidator samlResponseSignatureValidator;
    private final List<AssertionDecrypter> assertionDecrypters;
    private final SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    private final EncryptedResponseFromIdpValidator responseFromIdpValidator;
    private final DestinationValidator responseDestinationValidator;
    private final ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator;
    private ValidatedResponse validatedResponse;
    private ValidatedAssertions validatedAssertions;

    public IdpResponseValidator(SamlResponseSignatureValidator samlResponseSignatureValidator,
                                List<AssertionDecrypter> assertionDecrypters,
                                SamlAssertionsSignatureValidator samlAssertionsSignatureValidator,
                                EncryptedResponseFromIdpValidator responseFromIdpValidator,
                                DestinationValidator responseDestinationValidator,
                                ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator) {
        this.samlResponseSignatureValidator = samlResponseSignatureValidator;
        this.assertionDecrypters = assertionDecrypters;
        this.samlAssertionsSignatureValidator = samlAssertionsSignatureValidator;
        this.responseFromIdpValidator = responseFromIdpValidator;
        this.responseDestinationValidator = responseDestinationValidator;
        this.responseAssertionsFromIdpValidator = responseAssertionsFromIdpValidator;
    }

    public ValidatedResponse getValidatedResponse() {
        return validatedResponse;
    }

    public ValidatedAssertions getValidatedAssertions() {
        return validatedAssertions;
    }

    public void validate(Response response) {
        responseFromIdpValidator.validate(response);
        responseDestinationValidator.validate(response.getDestination());

        validatedResponse = samlResponseSignatureValidator.validate(response, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
        List<Assertion> decryptedAssertions = null;

        for (AssertionDecrypter assertionDecrypter : assertionDecrypters) {
            try {
                decryptedAssertions = assertionDecrypter.decryptAssertions(validatedResponse);
            } catch(SamlFailedToDecryptException e) {
                log.warn("Failed to decrypt IDP assertions with one of the decrypters", e);
            }
        }

        if (decryptedAssertions == null) {
            throw new SamlFailedToDecryptException("Could not decrypt IDP assertions with any of the decrypters", Level.ERROR);
        }

        validatedAssertions = samlAssertionsSignatureValidator.validate(decryptedAssertions, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);

        responseAssertionsFromIdpValidator.validate(validatedResponse, validatedAssertions);
    }
}
