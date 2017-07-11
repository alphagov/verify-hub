package uk.gov.ida.saml.hub.transformers.inbound;

import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import uk.gov.ida.saml.security.validators.signature.SamlRequestSignatureValidator;
import uk.gov.ida.saml.core.transformers.inbound.decorators.ValidateSamlAuthnRequestFromTransactionDestination;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromTransaction;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestFromTransactionValidator;

import java.util.function.Function;

public class AuthnRequestToIdaRequestFromTransactionTransformer implements Function<AuthnRequest, AuthnRequestFromTransaction> {
    private final AuthnRequestFromTransactionUnmarshaller authnRequestFromTransactionUnmarshaller;
    private final SamlRequestSignatureValidator<AuthnRequest> samlRequestSignatureValidator;
    private final ValidateSamlAuthnRequestFromTransactionDestination validateSamlResponseDestination;
    private final AuthnRequestFromTransactionValidator authnRequestFromTransactionValidator;

    public AuthnRequestToIdaRequestFromTransactionTransformer(
            AuthnRequestFromTransactionUnmarshaller authnRequestFromTransactionUnmarshaller,
            SamlRequestSignatureValidator<AuthnRequest> samlRequestSignatureValidator,
            ValidateSamlAuthnRequestFromTransactionDestination validateSamlResponseDestination,
            AuthnRequestFromTransactionValidator authnRequestFromTransactionValidator) {

        this.authnRequestFromTransactionUnmarshaller = authnRequestFromTransactionUnmarshaller;
        this.samlRequestSignatureValidator = samlRequestSignatureValidator;
        this.validateSamlResponseDestination = validateSamlResponseDestination;
        this.authnRequestFromTransactionValidator = authnRequestFromTransactionValidator;
    }

    @Override
    public AuthnRequestFromTransaction apply(final AuthnRequest authnRequest) {
        authnRequestFromTransactionValidator.validate(authnRequest);
        validateSamlResponseDestination.validate(authnRequest);
        samlRequestSignatureValidator.validate(authnRequest, SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        return authnRequestFromTransactionUnmarshaller.fromSamlMessage(authnRequest);
    }

}
