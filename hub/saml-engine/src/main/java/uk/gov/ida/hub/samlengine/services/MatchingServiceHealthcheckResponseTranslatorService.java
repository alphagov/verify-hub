package uk.gov.ida.hub.samlengine.services;

import javax.inject.Inject;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.hub.samlengine.contracts.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.hub.samlengine.logging.MdcHelper;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.InboundHealthCheckResponseFromMatchingService;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer;

public class MatchingServiceHealthcheckResponseTranslatorService {

    // NOTE: this was an ElementTo... but using this transformer removes the need to
    // do a string to element ourselves before the transform
    private final StringToOpenSamlObjectTransformer<Response> responseUnmarshaller;
    private final DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer samlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer;

    @Inject
    public MatchingServiceHealthcheckResponseTranslatorService(
            StringToOpenSamlObjectTransformer<Response> responseUnmarshaller, DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer samlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer) {

        this.responseUnmarshaller = responseUnmarshaller;
        this.samlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer = samlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer;
    }

    public MatchingServiceHealthCheckerResponseDto translate(SamlMessageDto samlMessageDto) {
        Response response = responseUnmarshaller.apply(samlMessageDto.getSamlMessage());
        MdcHelper.addContextToMdc(response);
        final InboundHealthCheckResponseFromMatchingService responseFromMatchingService =
                samlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer.transform(response);

        final MatchingServiceHealthCheckerResponseDto matchingServiceHealthCheckerResponseDto = new MatchingServiceHealthCheckerResponseDto(
                responseFromMatchingService.getStatus(),
                responseFromMatchingService.getInResponseTo(),
                responseFromMatchingService.getIssuer(),
                responseFromMatchingService.getId());

        return matchingServiceHealthCheckerResponseDto;
    }

}
