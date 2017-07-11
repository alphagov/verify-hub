package uk.gov.ida.saml.hub.transformers.inbound.providers;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.hub.transformers.inbound.IdaResponseFromIdpUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.decorators.ValidateSamlResponseIssuedByIdpDestination;
import uk.gov.ida.saml.hub.validators.response.ResponseAssertionsFromIdpValidator;
import uk.gov.ida.saml.hub.validators.response.ResponseFromIdpValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import java.util.List;
import java.util.function.Function;

public class DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer implements Function<Response, InboundResponseFromIdp> {

    private final IdaResponseFromIdpUnmarshaller idaResponseUnmarshaller;
    private final SamlResponseSignatureValidator samlResponseSignatureValidator;
    private final AssertionDecrypter assertionDecrypter;
    private final SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    private final ResponseFromIdpValidator responseFromIdpValidator;
    private final ValidateSamlResponseIssuedByIdpDestination validateSamlResponseDestination;
    private final ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator;

    public DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(
            IdaResponseFromIdpUnmarshaller idaResponseUnmarshaller,
            SamlResponseSignatureValidator samlResponseSignatureValidator,
            AssertionDecrypter assertionDecrypter,
            SamlAssertionsSignatureValidator samlAssertionsSignatureValidator,
            ResponseFromIdpValidator responseFromIdpValidator,
            ValidateSamlResponseIssuedByIdpDestination validateSamlResponseDestination,
            ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator) {

        this.idaResponseUnmarshaller = idaResponseUnmarshaller;
        this.samlResponseSignatureValidator = samlResponseSignatureValidator;
        this.assertionDecrypter = assertionDecrypter;
        this.samlAssertionsSignatureValidator = samlAssertionsSignatureValidator;
        this.responseFromIdpValidator = responseFromIdpValidator;
        this.validateSamlResponseDestination = validateSamlResponseDestination;
        this.responseAssertionsFromIdpValidator = responseAssertionsFromIdpValidator;
    }

    @Override
    public InboundResponseFromIdp apply(Response response) {

        responseFromIdpValidator.validate(response);
        validateSamlResponseDestination.validate(response);


        ValidatedResponse validatedResponse = samlResponseSignatureValidator.validate(response, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
        List<Assertion> decryptedAssertions = assertionDecrypter.decryptAssertions(validatedResponse);
        ValidatedAssertions validatedAssertions = samlAssertionsSignatureValidator.validate(decryptedAssertions, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);

        responseAssertionsFromIdpValidator.validate(validatedResponse, validatedAssertions);

        return idaResponseUnmarshaller.fromSaml(validatedResponse, validatedAssertions);
    }

}
