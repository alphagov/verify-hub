package uk.gov.ida.saml.hub.transformers.inbound.providers;

import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.eidas.logging.EidasAttributesLogger;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;
import uk.gov.ida.saml.hub.transformers.inbound.IdaResponseFromIdpUnmarshaller;
import uk.gov.ida.saml.hub.validators.response.idp.IdpResponseValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.EncryptedResponseFromIdpValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.ResponseAssertionsFromIdpValidator;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import java.util.Optional;
import java.util.function.Function;

public class DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer implements Function<Response, InboundResponseFromIdp> {

    private final IdaResponseFromIdpUnmarshaller idaResponseUnmarshaller;
    private IdpResponseValidator idpResponseValidator;
    private Optional<EidasAttributesLogger> eidasAttributesLogger = Optional.empty();

    @Deprecated
    public DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(
            IdaResponseFromIdpUnmarshaller idaResponseUnmarshaller,
            SamlResponseSignatureValidator samlResponseSignatureValidator,
            AssertionDecrypter assertionDecrypter,
            SamlAssertionsSignatureValidator samlAssertionsSignatureValidator,
            EncryptedResponseFromIdpValidator responseFromIdpValidator,
            DestinationValidator responseDestinationValidator,
            ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator) {

        this(new IdpResponseValidator(
            samlResponseSignatureValidator,
            assertionDecrypter,
            samlAssertionsSignatureValidator,
            responseFromIdpValidator,
            responseDestinationValidator,
            responseAssertionsFromIdpValidator),
            idaResponseUnmarshaller);
    }

    public DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(IdpResponseValidator idpResponseValidator,
                                                                    IdaResponseFromIdpUnmarshaller idaResponseUnmarshaller){
        this.idaResponseUnmarshaller = idaResponseUnmarshaller;
        this.idpResponseValidator = idpResponseValidator;
    }

    @Override
    public InboundResponseFromIdp apply(Response response) {
        this.idpResponseValidator.validate(response);
        ValidatedResponse validatedResponse = this.idpResponseValidator.getValidatedResponse();
        ValidatedAssertions validatedAssertions = this.idpResponseValidator.getValidatedAssertions();
        eidasAttributesLogger.ifPresent(logger -> validatedAssertions.getMatchingDatasetAssertion()
                .ifPresent(assertion -> logger.logEidasAttributesAsHash(assertion, response)));
        return idaResponseUnmarshaller.fromSaml(validatedResponse, validatedAssertions);
    }

    public void setEidasAttributesLogger(EidasAttributesLogger eidasAttributesLogger) {
        this.eidasAttributesLogger = Optional.of(eidasAttributesLogger);
    }
}
