package uk.gov.ida.saml.hub.transformers.inbound.providers;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import uk.gov.ida.saml.hub.validators.response.matchingservice.EncryptedResponseFromMatchingServiceValidator;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.hub.domain.InboundResponseFromMatchingService;
import uk.gov.ida.saml.hub.transformers.inbound.InboundResponseFromMatchingServiceUnmarshaller;
import uk.gov.ida.saml.hub.validators.response.matchingservice.ResponseAssertionsFromMatchingServiceValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import java.util.List;

public class DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer {

    private final InboundResponseFromMatchingServiceUnmarshaller responseUnmarshaller;
    private final SamlResponseSignatureValidator samlResponseSignatureValidator;
    private final AssertionDecrypter samlResponseAssertionDecrypter;
    private final SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    private final EncryptedResponseFromMatchingServiceValidator responseFromMatchingServiceValidator;
    private final ResponseAssertionsFromMatchingServiceValidator responseAssertionsFromMatchingServiceValidator;

    public DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer(
            InboundResponseFromMatchingServiceUnmarshaller responseUnmarshaller,
            SamlResponseSignatureValidator samlResponseSignatureValidator,
            AssertionDecrypter samlResponseAssertionDecrypter,
            SamlAssertionsSignatureValidator samlAssertionsSignatureValidator,
            EncryptedResponseFromMatchingServiceValidator responseFromMatchingServiceValidator,
            ResponseAssertionsFromMatchingServiceValidator responseAssertionsFromMatchingServiceValidator) {

        this.responseUnmarshaller = responseUnmarshaller;
        this.samlResponseSignatureValidator = samlResponseSignatureValidator;
        this.samlResponseAssertionDecrypter = samlResponseAssertionDecrypter;
        this.samlAssertionsSignatureValidator = samlAssertionsSignatureValidator;
        this.responseFromMatchingServiceValidator = responseFromMatchingServiceValidator;
        this.responseAssertionsFromMatchingServiceValidator = responseAssertionsFromMatchingServiceValidator;
    }

    public InboundResponseFromMatchingService transform(Response response) {
        responseFromMatchingServiceValidator.validate(response);

        /* Decrypt and validate assertions independently. */
        ValidatedResponse validatedResponse = samlResponseSignatureValidator.validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
        /* Decrypt and validate assertions independently. */
        List<Assertion> decryptedAssertions = samlResponseAssertionDecrypter.decryptAssertions(validatedResponse);
        ValidatedAssertions validatedAssertions = samlAssertionsSignatureValidator.validate(decryptedAssertions, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);

        responseAssertionsFromMatchingServiceValidator.validate(validatedResponse, validatedAssertions);

        return responseUnmarshaller.fromSaml(validatedResponse, validatedAssertions);
    }
}
