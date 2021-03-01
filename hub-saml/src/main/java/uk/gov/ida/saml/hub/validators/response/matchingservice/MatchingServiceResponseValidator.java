package uk.gov.ida.saml.hub.validators.response.matchingservice;

import io.prometheus.client.Counter;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import uk.gov.ida.saml.core.security.AssertionsDecrypters;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

public class MatchingServiceResponseValidator {
    private final EncryptedResponseFromMatchingServiceValidator responseFromMatchingServiceValidator;
    private final SamlResponseSignatureValidator samlResponseSignatureValidator;
    private final AssertionsDecrypters assertionsDecrypters;
    private final SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    private final ResponseAssertionsFromMatchingServiceValidator responseAssertionsFromMatchingServiceValidator;
    private ValidatedResponse validatedResponse;
    private ValidatedAssertions validatedAssertions;
    
    private static final Counter msaDecryptionErrorCounter = Counter.build(
            "verify_saml_hub_msa_validator_decryption_error_counter",
            "MSA Decryption error counter, reports number of errors by MSA")
            .labelNames("entityId")
            .register();
    
    public MatchingServiceResponseValidator(
        EncryptedResponseFromMatchingServiceValidator responseFromMatchingServiceValidator,
        SamlResponseSignatureValidator samlResponseSignatureValidator,
        AssertionsDecrypters assertionsDecrypters,
        SamlAssertionsSignatureValidator samlAssertionsSignatureValidator,
        ResponseAssertionsFromMatchingServiceValidator responseAssertionsFromMatchingServiceValidator) {
        this.responseFromMatchingServiceValidator = responseFromMatchingServiceValidator;
        this.samlResponseSignatureValidator = samlResponseSignatureValidator;
        this.assertionsDecrypters = assertionsDecrypters;
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
        
        var decryptedAssertions = assertionsDecrypters.decryptedAssertions(
            validatedResponse,
            msaDecryptionErrorCounter,
            MatchingServiceResponseValidator.class  
        );

        validatedAssertions = samlAssertionsSignatureValidator.validate(decryptedAssertions, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);

        responseAssertionsFromMatchingServiceValidator.validate(validatedResponse, validatedAssertions);
    }
}
