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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    private AuthnResponseFromHubContainerDto createSuccessResponse(final ResponseFromHubDto responseFromHub) {
        String authnRequestIssuerEntityId = responseFromHub.getAuthnRequestIssuerEntityId();

        List<String> encryptedAssertions = responseFromHub.getEncryptedAssertions();
        if (encryptedAssertions == null || encryptedAssertions.isEmpty()) {
            encryptedAssertions = responseFromHub.getEncryptedMatchingServiceAssertion()
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        }

        final OutboundResponseFromHub response = new OutboundResponseFromHub(
                responseFromHub.getResponseId(),
                responseFromHub.getInResponseTo(),
                hubEntityId,
                DateTime.now(),
                TransactionIdaStatus.valueOf(responseFromHub.getStatus().name()),
                encryptedAssertions,
                responseFromHub.getAssertionConsumerServiceUri());

        String samlMessage = outboundResponseFromHubToResponseTransformerFactory.get(authnRequestIssuerEntityId).apply(response);

        return new AuthnResponseFromHubContainerDto(
                samlMessage,
                responseFromHub.getAssertionConsumerServiceUri(),
                responseFromHub.getRelayState(),
                responseFromHub.getResponseId());
    }

}
