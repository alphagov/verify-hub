package uk.gov.ida.hub.samlengine.services;

import org.joda.time.DateTime;
import org.slf4j.event.Level;
import uk.gov.ida.hub.samlengine.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlengine.contracts.ResponseFromHubDto;
import uk.gov.ida.hub.samlengine.exceptions.UnableToGenerateSamlException;
import uk.gov.ida.hub.samlengine.factories.OutboundResponseFromHubToResponseTransformerFactory;
import uk.gov.ida.hub.samlengine.locators.AssignableEntityToEncryptForLocator;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;

import javax.inject.Inject;
import javax.inject.Named;

public class RpAuthnResponseGeneratorService {

    private final AssignableEntityToEncryptForLocator assignableEntityToEncryptForLocator;
    private final OutboundResponseFromHubToResponseTransformerFactory outboundResponseFromHubToResponseTransformerFactory;
    private final String hubEntityId;

    @Inject
    public RpAuthnResponseGeneratorService(AssignableEntityToEncryptForLocator assignableEntityToEncryptForLocator,
                                           OutboundResponseFromHubToResponseTransformerFactory outboundResponseFromHubToResponseTransformerFactory,
                                           @Named("HubEntityId") String hubEntityId) {

        this.assignableEntityToEncryptForLocator = assignableEntityToEncryptForLocator;
        this.outboundResponseFromHubToResponseTransformerFactory = outboundResponseFromHubToResponseTransformerFactory;
        this.hubEntityId = hubEntityId;
    }

    public AuthnResponseFromHubContainerDto generate(ResponseFromHubDto responseFromHub) {
        String entityIdForEncryption = responseFromHub.getAuthnRequestIssuerEntityId();
        String originalRequestId = responseFromHub.getInResponseTo();
        try{
            assignableEntityToEncryptForLocator.addEntityIdForRequestId(originalRequestId, entityIdForEncryption);
            return createSuccessResponse(responseFromHub);
        } catch (Exception e) {
            throw new UnableToGenerateSamlException("Unable to generate RP authn response", e, Level.ERROR);
        } finally {
            assignableEntityToEncryptForLocator.removeEntityIdForRequestId(originalRequestId);
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
                responseFromHub.getEncryptedMatchingServiceAssertion(),
                responseFromHub.getAssertionConsumerServiceUri());

        String samlMessage = outboundResponseFromHubToResponseTransformerFactory.get(authnRequestIssuerEntityId).apply(response);

        return new AuthnResponseFromHubContainerDto(
                samlMessage,
                responseFromHub.getAssertionConsumerServiceUri(),
                responseFromHub.getRelayState(),
                responseFromHub.getResponseId());
    }

}
