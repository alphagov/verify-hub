package uk.gov.ida.saml.hub.validators.response.idp;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.EncryptedResponseFromIdpValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.ResponseAssertionsFromIdpValidator;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import java.util.List;

public class IdpResponseValidator {
    private final SamlResponseSignatureValidator samlResponseSignatureValidator;
    private final AssertionDecrypter assertionDecrypter;
    private final SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    private final EncryptedResponseFromIdpValidator responseFromIdpValidator;
    private final DestinationValidator responseDestinationValidator;
    private final ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator;
    private ValidatedResponse validatedResponse;
    private ValidatedAssertions validatedAssertions;

    public IdpResponseValidator(SamlResponseSignatureValidator samlResponseSignatureValidator,
                                AssertionDecrypter assertionDecrypter,
                                SamlAssertionsSignatureValidator samlAssertionsSignatureValidator,
                                EncryptedResponseFromIdpValidator responseFromIdpValidator,
                                DestinationValidator responseDestinationValidator,
                                ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator) {
        this.samlResponseSignatureValidator = samlResponseSignatureValidator;
        this.assertionDecrypter = assertionDecrypter;
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

        List<Assertion> decryptedAssertions = assertionDecrypter.decryptAssertions(validatedResponse);
        validatedAssertions = samlAssertionsSignatureValidator.validate(decryptedAssertions, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);

        responseAssertionsFromIdpValidator.validate(validatedResponse, validatedAssertions);
    }
}
