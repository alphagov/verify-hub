package uk.gov.ida.hub.samlproxy.logging;

import com.google.common.collect.ImmutableMap;
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
        MDC.setContextMap(ImmutableMap.<String, String>builder()
                .put("requestId", request.getID())
                .put("direction", direction.name())
                .put("issuerId", Optional.ofNullable(request.getIssuer()).map(Issuer::getValue).orElse("NoIssuer"))
                .put("destination", Optional.ofNullable(request.getDestination()).orElse("NoDestination"))
                .put("signatureStatus", signatureStatus.name())
                .build());
        LOG.info(formatter.formatAuthnRequest(request, direction, signatureStatus));
        MDC.clear();
        MDC.setContextMap(copyOfContextMap);
    }

    public void logAuthnResponse(Response samlResponse, Direction direction, SignatureStatus signatureStatus) {
        Map<String, String> copyOfContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(Collections.emptyMap());
        StatusCode statusCode = samlResponse.getStatus().getStatusCode();
        MDC.setContextMap(ImmutableMap.<String, String>builder()
                .put("responseId", samlResponse.getID())
                .put("inResponseTo", samlResponse.getInResponseTo())
                .put("status", statusCode.getValue())
                .put("subStatus", Optional.ofNullable(statusCode.getStatusCode()).map(StatusCode::getValue).orElse("NoSubStatus"))
                .put("direction", direction.name())
                .put("issuerId", Optional.ofNullable(samlResponse.getIssuer()).map(Issuer::getValue).orElse("NoIssuer"))
                .put("destination", Optional.ofNullable(samlResponse.getDestination()).orElse("NoDestination"))
                .put("signatureStatus", signatureStatus.name())
                .build());
        LOG.info(formatter.formatAuthnResponse(samlResponse, direction, signatureStatus));
        MDC.clear();
        MDC.setContextMap(copyOfContextMap);
    }

}
