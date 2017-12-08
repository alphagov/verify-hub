package uk.gov.ida.hub.samlsoapproxy.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectiveMonitoringLogger {
    private static final Logger LOG = LoggerFactory.getLogger(ProtectiveMonitoringLogger.class);
    private static final String ATTRIBUTE_QUERY = "Protective Monitoring – Attribute Query Event – {requestId: {}, destination: {}, issuerId: {}, validSignature: {}}";
    private static final String ATTRIBUTE_QUERY_RESPONSE = "Protective Monitoring – Attribute Query Response Event – {responseId: {}, inResponseTo: {}, issuerId: {}, validSignature: {}, status: {}, statusMessage: {}}";

    public void logAttributeQuery(String requestId, String destination, String issuer, boolean validSignature) {
        LOG.info(ATTRIBUTE_QUERY, requestId, destination, issuer, validSignature);
    }

    public void logAttributeQueryResponse(String requestId, String inResponseTo, String issuer, boolean validSignature, String status, String statusMessage) {
        LOG.info(ATTRIBUTE_QUERY_RESPONSE, requestId, inResponseTo, issuer, validSignature, status, statusMessage);
    }
}
