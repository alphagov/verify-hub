package uk.gov.ida.hub.samlsoapproxy.runnabletasks;

import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlsoapproxy.client.AttributeQueryRequestClient;
import uk.gov.ida.hub.samlsoapproxy.domain.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlsoapproxy.exceptions.InvalidSamlRequestInAttributeQueryException;
import uk.gov.ida.hub.samlsoapproxy.logging.ProtectiveMonitoringLogger;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationResponse;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

public class ExecuteAttributeQueryRequest {

    private static final Logger LOG = LoggerFactory.getLogger(ExecuteAttributeQueryRequest.class);

    private final Function<Element, AttributeQuery> elementToAttributeQueryTransformer;
    private final Function<Element, Response> elementToSamlResponseTransformer;
    private final SamlMessageSignatureValidator matchingRequestSignatureValidator;
    private final SamlMessageSignatureValidator matchingResponseSignatureValidator;
    private final AttributeQueryRequestClient attributeQueryRequestClient;
    private final ProtectiveMonitoringLogger protectiveMonitoringLogger;

    @Inject
    public ExecuteAttributeQueryRequest(Function<Element, AttributeQuery> elementToAttributeQueryTransformer,
                                        Function<Element, Response> elementToSamlResponseTransformer,
                                        @Named("matchingRequestSignatureValidator") SamlMessageSignatureValidator matchingRequestSignatureValidator,
                                        @Named("matchingResponseSignatureValidator") SamlMessageSignatureValidator matchingResponseSignatureValidator,
                                        AttributeQueryRequestClient attributeQueryRequestClient,
                                        ProtectiveMonitoringLogger protectiveMonitoringLogger) {
        this.elementToAttributeQueryTransformer = elementToAttributeQueryTransformer;
        this.elementToSamlResponseTransformer = elementToSamlResponseTransformer;
        this.matchingRequestSignatureValidator = matchingRequestSignatureValidator;
        this.matchingResponseSignatureValidator = matchingResponseSignatureValidator;
        this.attributeQueryRequestClient = attributeQueryRequestClient;
        this.protectiveMonitoringLogger = protectiveMonitoringLogger;
    }

    public Element execute(SessionId sessionId, AttributeQueryContainerDto attributeQueryContainerDto) {

        LOG.info("Validating attribute query {}", attributeQueryContainerDto.getId());
        Element matchingServiceRequest = convertToElementAndValidate(attributeQueryContainerDto);

        LOG.info("Sending attribute query {}", attributeQueryContainerDto.getId());
        final Element responseFromMatchingService = attributeQueryRequestClient.sendQuery(
                matchingServiceRequest,
                attributeQueryContainerDto.getId(),
                sessionId,
                attributeQueryContainerDto.getMatchingServiceUri());

        validateResponseSignature(responseFromMatchingService);

        return responseFromMatchingService;
    }

    private Element convertToElementAndValidate(AttributeQueryContainerDto attributeQueryContainerDto) {
        try {
            Element matchingServiceRequest;
            matchingServiceRequest = XmlUtils.convertToElement(attributeQueryContainerDto.getSamlRequest());
            validateRequestSignature(matchingServiceRequest, attributeQueryContainerDto.getMatchingServiceUri());
            return matchingServiceRequest;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new InvalidSamlRequestInAttributeQueryException("Attribute Query had invalid XML.", e);
        } catch (SamlTransformationErrorException e) {
            throw new InvalidSamlRequestInAttributeQueryException("Attribute Query had invalid Saml", e);
        }
    }

    private void validateRequestSignature(Element matchingServiceRequest, URI matchingServiceUri) {
        AttributeQuery attributeQuery = elementToAttributeQueryTransformer.apply(matchingServiceRequest);
        SamlValidationResponse signatureValidationResponse = matchingRequestSignatureValidator.validate(attributeQuery, SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        protectiveMonitoringLogger.logAttributeQuery(attributeQuery.getID(), matchingServiceUri.toASCIIString(), attributeQuery.getIssuer().getValue(), signatureValidationResponse.isOK());
        if (!signatureValidationResponse.isOK()) {
            SamlValidationSpecificationFailure failure = signatureValidationResponse.getSamlValidationSpecificationFailure();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), signatureValidationResponse.getCause(), Level.ERROR);
        }
    }

    private void validateResponseSignature(Element responseFromMatchingService) {
        Response response = elementToSamlResponseTransformer.apply(responseFromMatchingService);
        SamlValidationResponse signatureValidationResponse = matchingResponseSignatureValidator.validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
        String message = hasStatusMessage(response.getStatus()) ? response.getStatus().getStatusMessage().getMessage() : "";
        protectiveMonitoringLogger.logAttributeQueryResponse(response.getID(), response.getInResponseTo(), response.getIssuer().getValue(),
                signatureValidationResponse.isOK(), response.getStatus().getStatusCode().getValue(), message);
        if (!signatureValidationResponse.isOK()) {
            SamlValidationSpecificationFailure failure = signatureValidationResponse.getSamlValidationSpecificationFailure();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), signatureValidationResponse.getCause(), Level.ERROR);
        }
    }

    private boolean hasStatusMessage(final Status status) {
        return status.getStatusMessage() != null;
    }
}
