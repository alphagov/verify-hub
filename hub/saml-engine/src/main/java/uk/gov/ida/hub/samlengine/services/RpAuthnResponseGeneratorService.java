package uk.gov.ida.hub.samlengine.services;

import org.joda.time.DateTime;
import org.slf4j.event.Level;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.hub.samlengine.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlengine.contracts.ResponseFromHubDto;
import uk.gov.ida.hub.samlengine.exceptions.UnableToGenerateSamlException;
import uk.gov.ida.hub.samlengine.factories.OutboundResponseFromHubToResponseTransformerFactory;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;

import javax.inject.Inject;
import javax.inject.Named;

public class RpAuthnResponseGeneratorService {

    private final OutboundResponseFromHubToResponseTransformerFactory outboundResponseFromHubToResponseTransformerFactory;
    private final String hubEntityId;

    @Inject
    public RpAuthnResponseGeneratorService(OutboundResponseFromHubToResponseTransformerFactory outboundResponseFromHubToResponseTransformerFactory,
                                           @Named("HubEntityId") String hubEntityId) {
        this.outboundResponseFromHubToResponseTransformerFactory = outboundResponseFromHubToResponseTransformerFactory;
        this.hubEntityId = hubEntityId;
    }

    public AuthnResponseFromHubContainerDto generate(ResponseFromHubDto responseFromHub) {
        try{
            return createSuccessResponse(responseFromHub);
        } catch (Exception e) {
            throw new UnableToGenerateSamlException("Unable to generate RP authn response", e, Level.ERROR);
        }
    }

    public AuthnResponseFromHubContainerDto generate(AuthnResponseFromCountryContainerDto responseFromHub) {
        try{
            return createSuccessResponseFromCountryDto(responseFromHub);
        } catch (Exception e) {
            throw new UnableToGenerateSamlException("Unable to generate RP authn response", e, Level.ERROR);
        }
    }

    private AuthnResponseFromHubContainerDto createSuccessResponse(final ResponseFromHubDto responseFromHub) {
        String authnRequestIssuerEntityId = responseFromHub.getAuthnRequestIssuerEntityId();

        final OutboundResponseFromHub response = new OutboundResponseFromHub(
                responseFromHub.getResponseId(),
                responseFromHub.getInResponseTo(),
                hubEntityId,
                DateTime.now(),
                TransactionIdaStatus.valueOf(responseFromHub.getStatus().name()),
                responseFromHub.getEncryptedAssertions(),
                responseFromHub.getAssertionConsumerServiceUri());

        String samlMessage = outboundResponseFromHubToResponseTransformerFactory.get(authnRequestIssuerEntityId).apply(response);

        return new AuthnResponseFromHubContainerDto(
                samlMessage,
                responseFromHub.getAssertionConsumerServiceUri(),
                responseFromHub.getRelayState(),
                responseFromHub.getResponseId());
    }

    private AuthnResponseFromHubContainerDto createSuccessResponseFromCountryDto(AuthnResponseFromCountryContainerDto responseFromCountryDto) {
        String samlMessage = outboundResponseFromHubToResponseTransformerFactory.getCountryTransformer().apply(responseFromCountryDto);
        return new AuthnResponseFromHubContainerDto(
                samlMessage,
                responseFromCountryDto.getPostEndpoint(),
                responseFromCountryDto.getRelayState(),
                responseFromCountryDto.getResponseId());
    }

}
