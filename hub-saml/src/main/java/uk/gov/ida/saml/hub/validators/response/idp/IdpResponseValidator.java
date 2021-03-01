package uk.gov.ida.saml.hub.validators.response.idp;

import io.prometheus.client.Counter;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import uk.gov.ida.saml.core.security.AssertionsDecrypters;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.EncryptedResponseFromIdpValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.ResponseAssertionsFromIdpValidator;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

public class IdpResponseValidator {
    private final SamlResponseSignatureValidator samlResponseSignatureValidator;
    private final SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    private final EncryptedResponseFromIdpValidator<?> responseFromIdpValidator;
    private final DestinationValidator responseDestinationValidator;
    private final ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator;
    private final AssertionsDecrypters assertionsDecrypters;
    private ValidatedResponse validatedResponse;
    private ValidatedAssertions validatedAssertions;
    
    private static final Counter idpDecryptionErrorCounter = Counter.build(
            "verify_saml_hub_idp_validator_decryption_error_counter",
            "IDP Decryption error counter, reports number of errors by IDP")
            .labelNames("entityId")
            .register();
    
    public IdpResponseValidator(SamlResponseSignatureValidator samlResponseSignatureValidator,
                                AssertionsDecrypters assertionsDecrypters,
                                SamlAssertionsSignatureValidator samlAssertionsSignatureValidator,
                                EncryptedResponseFromIdpValidator<?> responseFromIdpValidator,
                                DestinationValidator responseDestinationValidator,
                                ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator) {
        this.samlResponseSignatureValidator = samlResponseSignatureValidator;
        this.assertionsDecrypters = assertionsDecrypters;
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

        var decryptedAssertions = assertionsDecrypters.decryptedAssertions(
                validatedResponse,
                idpDecryptionErrorCounter,
                IdpResponseValidator.class
        );
        
        validatedAssertions = samlAssertionsSignatureValidator.validate(decryptedAssertions, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);

        responseAssertionsFromIdpValidator.validate(validatedResponse, validatedAssertions);
    }
}
