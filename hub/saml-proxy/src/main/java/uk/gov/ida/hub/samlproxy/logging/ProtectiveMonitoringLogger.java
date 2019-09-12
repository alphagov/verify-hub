package uk.gov.ida.hub.samlproxy.logging;

import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.ida.hub.samlproxy.repositories.Direction;
import uk.gov.ida.hub.samlproxy.repositories.SignatureStatus;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ProtectiveMonitoringLogger {
    private static final Logger LOG = LoggerFactory.getLogger(ProtectiveMonitoringLogger.class);

    private final ProtectiveMonitoringLogFormatter formatter;

    @Inject
    public ProtectiveMonitoringLogger(ProtectiveMonitoringLogFormatter formatter) {
        this.formatter = formatter;
    }

    public void logAuthnRequest(AuthnRequest request, Direction direction, SignatureStatus signatureStatus) {
        Map<String, String> copyOfContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(Collections.emptyMap());
        MDC.setContextMap(Map.of(
                "requestId", request.getID(),
                "direction", direction.name(),
                "issuerId", Optional.ofNullable(request.getIssuer()).map(Issuer::getValue).orElse("NoIssuer"),
                "destination", Optional.ofNullable(request.getDestination()).orElse("NoDestination"),
                "signatureStatus", signatureStatus.name()));
        LOG.info(formatter.formatAuthnRequest(request, direction, signatureStatus));
        MDC.clear();
        MDC.setContextMap(copyOfContextMap);
    }

    public void logAuthnResponse(Response samlResponse, Direction direction, SignatureStatus signatureStatus) {
        Map<String, String> copyOfContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(Collections.emptyMap());
        StatusCode statusCode = samlResponse.getStatus().getStatusCode();
        MDC.setContextMap(Map.of(
                "responseId", samlResponse.getID(),
                "inResponseTo", samlResponse.getInResponseTo(),
                "status", statusCode.getValue(),
                "subStatus", Optional.ofNullable(statusCode.getStatusCode()).map(StatusCode::getValue).orElse("NoSubStatus"),
                "direction", direction.name(),
                "issuerId", Optional.ofNullable(samlResponse.getIssuer()).map(Issuer::getValue).orElse("NoIssuer"),
                "destination", Optional.ofNullable(samlResponse.getDestination()).orElse("NoDestination"),
                "signatureStatus", signatureStatus.name()));
        LOG.info(formatter.formatAuthnResponse(samlResponse, direction, signatureStatus));
        MDC.clear();
        MDC.setContextMap(copyOfContextMap);
    }

}
