package uk.gov.ida.hub.samlengine.attributequery;

import org.joda.time.DateTime;
import org.slf4j.event.Level;
import org.w3c.dom.Element;
import uk.gov.ida.hub.samlengine.domain.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.hub.samlengine.exceptions.UnableToGenerateSamlException;
import uk.gov.ida.hub.samlengine.locators.AssignableEntityToEncryptForLocator;
import uk.gov.ida.saml.hub.domain.BaseHubAttributeQueryRequest;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.inject.Inject;
import java.net.URI;
import java.util.function.Function;

public class AttributeQueryGenerator<T extends BaseHubAttributeQueryRequest> {
    protected final Function<T, Element> attributeQueryRequestTransformer;
    protected final AssignableEntityToEncryptForLocator entityToEncryptForLocator;

    @Inject
    public AttributeQueryGenerator(
            final Function<T, Element> attributeQueryRequestTransformer,
            final AssignableEntityToEncryptForLocator entityToEncryptForLocator) {
        this.entityToEncryptForLocator = entityToEncryptForLocator;
        this.attributeQueryRequestTransformer = attributeQueryRequestTransformer;
    }

    public AttributeQueryContainerDto createAttributeQueryContainer(final T attributeQueryRequest,
                                                                    final URI msaUri,
                                                                    final String matchingServiceEntityId,
                                                                    final DateTime matchingServiceRequestTimeOut,
                                                                    boolean onboarding) {

        try {
            entityToEncryptForLocator.addEntityIdForRequestId(
                    attributeQueryRequest.getId(),
                    matchingServiceEntityId);

            String samlRequest =
                    XmlUtils.writeToString(attributeQueryRequestTransformer.apply(attributeQueryRequest));
            return new AttributeQueryContainerDto(
                    attributeQueryRequest.getId(),
                    attributeQueryRequest.getIssuer(),
                    samlRequest,
                    msaUri,
                    matchingServiceRequestTimeOut,
                    onboarding);
        } finally {
            entityToEncryptForLocator.removeEntityIdForRequestId(attributeQueryRequest.getId());
        }
    }

    public uk.gov.ida.hub.samlengine.contracts.AttributeQueryContainerDto newCreateAttributeQueryContainer(final T attributeQueryRequest,
                                                                                                           final URI msaUri,
                                                                                                           final String matchingServiceEntityId,
                                                                                                           final DateTime matchingServiceRequestTimeOut,
                                                                                                           boolean onboarding) {

        try {
            entityToEncryptForLocator.addEntityIdForRequestId(
                    attributeQueryRequest.getId(),
                    matchingServiceEntityId);

            String samlRequest =
                    XmlUtils.writeToString(attributeQueryRequestTransformer.apply(attributeQueryRequest));
            return new uk.gov.ida.hub.samlengine.contracts.AttributeQueryContainerDto(
                    attributeQueryRequest.getId(),
                    attributeQueryRequest.getIssuer(),
                    samlRequest,
                    msaUri,
                    matchingServiceRequestTimeOut,
                    onboarding);
        } catch (Exception e) {
            throw new UnableToGenerateSamlException("failed to create attribute query request", e, Level.ERROR);
        } finally {
            entityToEncryptForLocator.removeEntityIdForRequestId(attributeQueryRequest.getId());
        }
    }

    public SamlMessageDto createAttributeQueryContainer(T attributeQueryRequest, String matchingServiceEntityId) {
        String requestId = attributeQueryRequest.getId();

        try {
            entityToEncryptForLocator.addEntityIdForRequestId(requestId, matchingServiceEntityId);
            return new SamlMessageDto(XmlUtils.writeToString(attributeQueryRequestTransformer.apply(attributeQueryRequest)));
        } catch (Exception e) {
            throw new UnableToGenerateSamlException("failed to create attribute query request", e, Level.ERROR);
        } finally {
            entityToEncryptForLocator.removeEntityIdForRequestId(requestId);
        }
    }
}
