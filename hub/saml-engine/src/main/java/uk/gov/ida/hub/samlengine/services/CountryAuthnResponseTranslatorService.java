package uk.gov.ida.hub.samlengine.services;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.samlengine.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.logging.MdcHelper;
import uk.gov.ida.hub.samlengine.validation.country.ResponseAssertionsFromCountryValidator;
import uk.gov.ida.hub.samlengine.validation.country.ResponseFromCountryValidator;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionBlobEncrypter;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.saml.hub.transformers.inbound.IdpIdaStatusUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.PassthroughAssertionUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.decorators.ValidateSamlResponseIssuedByIdpDestination;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class CountryAuthnResponseTranslatorService {

    private final StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer;
    private final ResponseFromCountryValidator responseFromCountryValidator;
    private final ValidateSamlResponseIssuedByIdpDestination responseFromCountryDestinationValidator;
    private final IdpIdaStatusUnmarshaller statusUnmarshaller;
    private final SamlResponseSignatureValidator responseSignatureValidator;
    private final AssertionDecrypter assertionDecrypter;
    private final AssertionBlobEncrypter assertionBlobEncrypter;
    private final PassthroughAssertionUnmarshaller passthroughAssertionUnmarshaller;
    private final SamlAssertionsSignatureValidator assertionSignatureValidator;
    private final ResponseAssertionsFromCountryValidator responseAssertionFromCountryValidator;

    @Inject
    public CountryAuthnResponseTranslatorService(StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer,
                                                 ResponseFromCountryValidator responseFromCountryValidator,
                                                 IdpIdaStatusUnmarshaller idpIdaStatusUnmarshaller,
                                                 ResponseAssertionsFromCountryValidator responseAssertionFromCountryValidator,
                                                 ValidateSamlResponseIssuedByIdpDestination validateSamlResponseIssuedByIdpDestination,
                                                 AssertionDecrypter assertionDecrypter,
                                                 AssertionBlobEncrypter assertionBlobEncrypter,
                                                 SamlResponseSignatureValidator responseSignatureValidator,
                                                 SamlAssertionsSignatureValidator assertionSignatureValidator,
                                                 PassthroughAssertionUnmarshaller passthroughAssertionUnmarshaller) {
        this.stringToOpenSamlResponseTransformer = stringToOpenSamlResponseTransformer;
        this.responseFromCountryValidator = responseFromCountryValidator;
        this.responseAssertionFromCountryValidator = responseAssertionFromCountryValidator;
        this.responseFromCountryDestinationValidator = validateSamlResponseIssuedByIdpDestination;
        this.statusUnmarshaller = idpIdaStatusUnmarshaller;
        this.assertionDecrypter = assertionDecrypter;
        this.assertionBlobEncrypter = assertionBlobEncrypter;
        this.responseSignatureValidator = responseSignatureValidator;
        this.assertionSignatureValidator = assertionSignatureValidator;
        this.passthroughAssertionUnmarshaller = passthroughAssertionUnmarshaller;
    }

    public InboundResponseFromCountry translate(SamlAuthnResponseTranslatorDto samlResponseDto) {
        Response response = unmarshall(samlResponseDto);
        ValidatedResponse validatedResponse = validateResponse(response);
        List<Assertion> assertions = assertionDecrypter.decryptAssertions(validatedResponse);
        Assertion validatedIdentityAssertion = validateAssertion(validatedResponse, assertions);
        return toModel(validatedResponse, validatedIdentityAssertion, samlResponseDto.getMatchingServiceEntityId());
    }

    private Response unmarshall(SamlAuthnResponseTranslatorDto dto) {
        Response response =  stringToOpenSamlResponseTransformer.apply(dto.getSamlResponse());
        MdcHelper.addContextToMdc(response);
        return response;
    }

    private ValidatedResponse validateResponse(Response response) {
        responseFromCountryValidator.validate(response);
        responseFromCountryDestinationValidator.validate(response);
        return responseSignatureValidator.validate(response, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    }

    private Assertion validateAssertion(ValidatedResponse validatedResponse, List<Assertion> decryptedAssertions) {
        assertionSignatureValidator.validate(decryptedAssertions, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
        Assertion identityAssertion = decryptedAssertions.get(0);
        responseAssertionFromCountryValidator.validate(validatedResponse, identityAssertion);
        return identityAssertion;
    }

    private InboundResponseFromCountry toModel(ValidatedResponse response, Assertion validatedIdentityAssertion, String matchingServiceEntityId) {

        PassthroughAssertion passthroughAssertion = passthroughAssertionUnmarshaller.fromAssertion(validatedIdentityAssertion, true);

        String loa  = passthroughAssertion.getAuthnContext().get().name();
        Optional<LevelOfAssurance> levelOfAssurance = Optional.empty();
        if (!Strings.isNullOrEmpty(loa)){
            levelOfAssurance = Optional.of(LevelOfAssurance.valueOf(loa));
        }

        IdpIdaStatus status = statusUnmarshaller.fromSaml(response.getStatus());

        return new InboundResponseFromCountry(
            response.getIssuer().getValue(),
            Optional.ofNullable(validatedIdentityAssertion).map(Assertion::getSubject).map(Subject::getNameID).map(NameID::getValue),
            Optional.ofNullable(status).map(IdpIdaStatus::getStatusCode).map(IdpIdaStatus.Status::name),
            status.getMessage(),
            Optional.ofNullable(assertionBlobEncrypter.encryptAssertionBlob(matchingServiceEntityId, passthroughAssertion.getUnderlyingAssertionBlob())),
            levelOfAssurance);
    }
}
