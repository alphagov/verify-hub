package uk.gov.ida.hub.samlengine.services;

import com.google.inject.Inject;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.hub.samlengine.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.domain.SamlResponseContainerDto;
import uk.gov.ida.hub.samlengine.logging.MdcHelper;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionBlobEncrypter;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.InboundResponseFromMatchingService;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer;

import java.util.Optional;

public class MatchingServiceResponseTranslatorService {

    // NOTE: this was an ElementTo... but using this transformer removes the need to
    // do a string to element ourselves before the transform
    private final StringToOpenSamlObjectTransformer<Response> responseUnmarshaller;
    private final DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer responseToInboundResponseFromMatchingServiceTransformer;
    private AssertionBlobEncrypter assertionBlobEncrypter;

    @Inject
    public MatchingServiceResponseTranslatorService(
            StringToOpenSamlObjectTransformer<Response> responseUnmarshaller,
            DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer responseToInboundResponseFromMatchingServiceTransformer,
             AssertionBlobEncrypter assertionBlobEncrypter) {

        this.responseUnmarshaller = responseUnmarshaller;
        this.responseToInboundResponseFromMatchingServiceTransformer = responseToInboundResponseFromMatchingServiceTransformer;
        this.assertionBlobEncrypter = assertionBlobEncrypter;
    }

    public InboundResponseFromMatchingServiceDto translate(SamlResponseContainerDto samlResponseContainerDto) {
        final Response response = responseUnmarshaller.apply(samlResponseContainerDto.getSamlResponse());
        MdcHelper.addContextToMdc(response);
        final InboundResponseFromMatchingService responseFromMatchingService = responseToInboundResponseFromMatchingServiceTransformer.transform(response);

        Optional<String> assertionBlob = Optional.empty();
        Optional<LevelOfAssurance> levelOfAssurance = Optional.empty();
        // FIXME?: transformer can return null
        if(responseFromMatchingService.getMatchingServiceAssertion()!=null && responseFromMatchingService.getMatchingServiceAssertion().isPresent()) {
            assertionBlob = Optional.ofNullable(responseFromMatchingService.getMatchingServiceAssertion().get().getUnderlyingAssertionBlob());
            final Optional<AuthnContext> authnContext = responseFromMatchingService.getMatchingServiceAssertion().get().getAuthnContext();
            if(authnContext.isPresent()) {
                levelOfAssurance = Optional.of(LevelOfAssurance.valueOf(authnContext.get().name()));
            }
        }

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = new InboundResponseFromMatchingServiceDto(
                responseFromMatchingService.getStatus(),
                responseFromMatchingService.getInResponseTo(),
                responseFromMatchingService.getIssuer(),
                assertionBlob.map(ab -> assertionBlobEncrypter.encryptAssertionBlob(samlResponseContainerDto.getAuthnRequestIssuerId(), ab)),
                levelOfAssurance);

        return inboundResponseFromMatchingServiceDto;
    }

}
