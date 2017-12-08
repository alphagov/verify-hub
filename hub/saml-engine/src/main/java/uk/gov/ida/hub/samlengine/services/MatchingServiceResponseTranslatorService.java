package uk.gov.ida.hub.samlengine.services;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.hub.samlengine.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.domain.SamlResponseDto;
import uk.gov.ida.hub.samlengine.logging.MdcHelper;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.InboundResponseFromMatchingService;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer;

public class MatchingServiceResponseTranslatorService {

    // NOTE: this was an ElementTo... but using this transformer removes the need to
    // do a string to element ourselves before the transform
    private final StringToOpenSamlObjectTransformer<Response> responseUnmarshaller;
    private final DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer responseToInboundResponseFromMatchingServiceTransformer;

    @Inject
    public MatchingServiceResponseTranslatorService(
            StringToOpenSamlObjectTransformer<Response> responseUnmarshaller,
            DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer responseToInboundResponseFromMatchingServiceTransformer) {

        this.responseUnmarshaller = responseUnmarshaller;
        this.responseToInboundResponseFromMatchingServiceTransformer = responseToInboundResponseFromMatchingServiceTransformer;
    }

    public InboundResponseFromMatchingServiceDto translate(SamlResponseDto samlResponseDto) {
        final Response response = responseUnmarshaller.apply(samlResponseDto.getSamlResponse());
        MdcHelper.addContextToMdc(response);
        final InboundResponseFromMatchingService responseFromMatchingService = responseToInboundResponseFromMatchingServiceTransformer.transform(response);

        Optional<String> assertionBlob = Optional.absent();
        Optional<LevelOfAssurance> levelOfAssurance = Optional.absent();
        // FIXME?: transformer can return null
        if(responseFromMatchingService.getMatchingServiceAssertion()!=null && responseFromMatchingService.getMatchingServiceAssertion().isPresent()) {
            assertionBlob = Optional.fromNullable(responseFromMatchingService.getMatchingServiceAssertion().get().getUnderlyingAssertionBlob());
            final Optional<AuthnContext> authnContext = responseFromMatchingService.getMatchingServiceAssertion().get().getAuthnContext();
            if(authnContext.isPresent()) {
                levelOfAssurance = Optional.of(LevelOfAssurance.valueOf(authnContext.get().name()));
            }
        }

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = new InboundResponseFromMatchingServiceDto(
                responseFromMatchingService.getStatus(),
                responseFromMatchingService.getInResponseTo(),
                responseFromMatchingService.getIssuer(),
                assertionBlob,
                levelOfAssurance);

        return inboundResponseFromMatchingServiceDto;
    }

}
