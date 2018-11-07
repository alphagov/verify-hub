package uk.gov.ida.hub.samlengine.services;

import org.joda.time.DateTime;
import org.slf4j.event.Level;
import uk.gov.ida.hub.samlengine.contracts.RequestForErrorResponseFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.hub.samlengine.exceptions.UnableToGenerateSamlException;
import uk.gov.ida.hub.samlengine.factories.OutboundResponseFromHubToResponseTransformerFactory;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Collections;

import static java.util.Optional.empty;

public class RpErrorResponseGeneratorService {

    private final OutboundResponseFromHubToResponseTransformerFactory outboundResponseFromHubToResponseTransformerFactory;
    private final String hubEntityId;

    @Inject
    public RpErrorResponseGeneratorService(OutboundResponseFromHubToResponseTransformerFactory outboundResponseFromHubToResponseTransformerFactory,
                                           @Named("HubEntityId") String hubEntityId) {

        this.outboundResponseFromHubToResponseTransformerFactory = outboundResponseFromHubToResponseTransformerFactory;
        this.hubEntityId = hubEntityId;
    }

    public SamlMessageDto generate(RequestForErrorResponseFromHubDto requestForErrorResponseFromHubDto) {

        try {
            final OutboundResponseFromHub response = new OutboundResponseFromHub(
                    requestForErrorResponseFromHubDto.getResponseId(),
                    requestForErrorResponseFromHubDto.getInResponseTo(),
                    hubEntityId,
                    DateTime.now(),
                    TransactionIdaStatus.valueOf(requestForErrorResponseFromHubDto.getStatus().name()),
                    Collections.emptyList(),
                    requestForErrorResponseFromHubDto.getAssertionConsumerServiceUri());

            final String errorResponse = outboundResponseFromHubToResponseTransformerFactory.get(requestForErrorResponseFromHubDto.getAuthnRequestIssuerEntityId()).apply(response);

            return new SamlMessageDto(errorResponse);
        } catch (Exception e) {
            throw new UnableToGenerateSamlException("Unable to generate RP error response", e, Level.ERROR);
        }

    }

}
