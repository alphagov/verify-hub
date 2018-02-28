package uk.gov.ida.hub.samlsoapproxy.logging;

import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class ProtectiveMonitoringLogger {
    private static final Logger LOG = LoggerFactory.getLogger(ProtectiveMonitoringLogger.class);
    private static final String ATTRIBUTE_QUERY = "Protective Monitoring – Attribute Query Event – {requestId: {}, destination: {}, issuerId: {}, validSignature: {}}";
    private static final String ATTRIBUTE_QUERY_RESPONSE = "Protective Monitoring – Attribute Query Response Event – {responseId: {}, inResponseTo: {}, issuerId: {}, validSignature: {}, status: {}, subStatus: {}, statusMessage: {}}";

    public void logAttributeQuery(AttributeQuery attributeQuery, URI matchingServiceUri, boolean ok) {
        logAttributeQuery(attributeQuery.getID(), matchingServiceUri.toASCIIString(), attributeQuery.getIssuer().getValue(), ok);
    }

    public void logAttributeQueryResponse(Response response, boolean validSignature) {
        Status status = response.getStatus();
        Optional<StatusCode> statusCode = ofNullable(status.getStatusCode());
        Optional<StatusCode> subStatusCode = statusCode.map(StatusCode::getStatusCode);

        logAttributeQueryResponse(
            response.getID(),
            response.getInResponseTo(),
            response.getIssuer().getValue(),
            validSignature,
            statusCode.map(StatusCode::getValue).orElse(""),
            subStatusCode.map(StatusCode::getValue).orElse(""),
            ofNullable(status.getStatusMessage()).map(StatusMessage::getMessage).orElse("")
        );
    }

    private void logAttributeQuery(String requestId, String destination, String issuer, boolean validSignature) {
        LOG.info(ATTRIBUTE_QUERY, requestId, destination, issuer, validSignature);
    }

    private void logAttributeQueryResponse(String requestId, String inResponseTo, String issuer, boolean validSignature, String status, String subStatus, String statusMessage) {
        LOG.info(ATTRIBUTE_QUERY_RESPONSE, requestId, inResponseTo, issuer, validSignature, status, subStatus, statusMessage);
    }

}
