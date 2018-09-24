package uk.gov.ida.hub.samlengine.services;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.samlengine.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.factories.EidasValidatorFactory;
import uk.gov.ida.hub.samlengine.logging.MdcHelper;
import uk.gov.ida.hub.samlengine.validation.country.ResponseAssertionsFromCountryValidator;
import uk.gov.ida.hub.samlengine.validation.country.ResponseFromCountryValidator;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionBlobEncrypter;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;
import uk.gov.ida.saml.hub.transformers.inbound.CountryAuthenticationStatusUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.PassthroughAssertionUnmarshaller;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

public class CountryAuthnResponseTranslatorService {

    private final StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer;
    private final ResponseFromCountryValidator responseFromCountryValidator;
    private final DestinationValidator responseFromCountryDestinationValidator;
    private final AssertionDecrypter assertionDecrypter;
    private final AssertionBlobEncrypter assertionBlobEncrypter;
    private final PassthroughAssertionUnmarshaller passthroughAssertionUnmarshaller;
    private final ResponseAssertionsFromCountryValidator responseAssertionFromCountryValidator;
    private final EidasValidatorFactory eidasValidatorFactory;
    private final CountryAuthenticationStatusUnmarshaller countryAuthenticationStatusUnmarshaller;

    @Inject
    public CountryAuthnResponseTranslatorService(StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer,
                                                 ResponseFromCountryValidator responseFromCountryValidator,
                                                 ResponseAssertionsFromCountryValidator responseAssertionFromCountryValidator,
                                                 DestinationValidator validateSamlResponseIssuedByIdpDestination,
                                                 AssertionDecrypter assertionDecrypter,
                                                 AssertionBlobEncrypter assertionBlobEncrypter,
                                                 EidasValidatorFactory eidasValidatorFactory,
                                                 PassthroughAssertionUnmarshaller passthroughAssertionUnmarshaller,
                                                 CountryAuthenticationStatusUnmarshaller countryAuthenticationStatusUnmarshaller) {
        this.stringToOpenSamlResponseTransformer = stringToOpenSamlResponseTransformer;
        this.responseFromCountryValidator = responseFromCountryValidator;
        this.responseAssertionFromCountryValidator = responseAssertionFromCountryValidator;
        this.responseFromCountryDestinationValidator = validateSamlResponseIssuedByIdpDestination;
        this.assertionDecrypter = assertionDecrypter;
        this.assertionBlobEncrypter = assertionBlobEncrypter;
        this.eidasValidatorFactory = eidasValidatorFactory;
        this.passthroughAssertionUnmarshaller = passthroughAssertionUnmarshaller;
        this.countryAuthenticationStatusUnmarshaller = countryAuthenticationStatusUnmarshaller;
    }

    public InboundResponseFromCountry translate(SamlAuthnResponseTranslatorDto samlResponseDto) {
        Response response = unmarshall(samlResponseDto);
        ValidatedResponse validatedResponse = validateResponse(response);
        List<Assertion> assertions = assertionDecrypter.decryptAssertions(validatedResponse);
        Optional<Assertion> validatedIdentityAssertion = validateAssertion(validatedResponse, assertions);
        return toModel(validatedResponse, validatedIdentityAssertion, samlResponseDto.getMatchingServiceEntityId());
    }

    private Response unmarshall(SamlAuthnResponseTranslatorDto dto) {
        Response response =  stringToOpenSamlResponseTransformer.apply(dto.getSamlResponse());
        MdcHelper.addContextToMdc(response);
        return response;
    }

    private ValidatedResponse validateResponse(Response response) {
        responseFromCountryValidator.validate(response);
        responseFromCountryDestinationValidator.validate(response.getDestination());
        return eidasValidatorFactory.getValidatedResponse(response);
    }

    private Optional<Assertion> validateAssertion(ValidatedResponse validatedResponse, List<Assertion> decryptedAssertions) {
        eidasValidatorFactory.getValidatedAssertion(validatedResponse, decryptedAssertions);
        Optional<Assertion> identityAssertion = decryptedAssertions.stream().findFirst();
        identityAssertion.ifPresent(assertion -> responseAssertionFromCountryValidator.validate(validatedResponse, assertion));
        return identityAssertion;
    }

    private InboundResponseFromCountry toModel(ValidatedResponse response, Optional<Assertion> validatedIdentityAssertionOptional, String matchingServiceEntityId) {

        Optional<PassthroughAssertion> passthroughAssertion = validatedIdentityAssertionOptional.map(validatedIdentityAssertion -> passthroughAssertionUnmarshaller.fromAssertion(validatedIdentityAssertion, true));

        Optional<LevelOfAssurance> levelOfAssurance = passthroughAssertion
                .flatMap(PassthroughAssertion::getAuthnContext)
                .map(AuthnContext::name)
                .filter(string -> !isNullOrEmpty(string))
                .map(LevelOfAssurance::valueOf);

        CountryAuthenticationStatus status = countryAuthenticationStatusUnmarshaller.fromSaml(response.getStatus());

        return new InboundResponseFromCountry(
            response.getIssuer().getValue(),
            validatedIdentityAssertionOptional.map(Assertion::getSubject).map(Subject::getNameID).map(NameID::getValue),
            Optional.ofNullable(status).map(CountryAuthenticationStatus::getStatusCode).map(CountryAuthenticationStatus.Status::name),
            status.getMessage(),
            passthroughAssertion.map(assertion -> assertionBlobEncrypter.encryptAssertionBlob(matchingServiceEntityId, assertion.getUnderlyingAssertionBlob())),
            levelOfAssurance);
    }
}
