package uk.gov.ida.hub.samlproxy.logging;

import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.samlproxy.repositories.Direction;

import javax.inject.Inject;

public class ProtectiveMonitoringLogger {
    private static final Logger LOG = LoggerFactory.getLogger(ProtectiveMonitoringLogger.class);

    private final ProtectiveMonitoringLogFormatter formatter;

    @Inject
    public ProtectiveMonitoringLogger(ProtectiveMonitoringLogFormatter formatter) {
        this.formatter = formatter;
    }

    public void logAuthnRequest(AuthnRequest request, Direction direction, Boolean validSignature) {
        LOG.info(formatter.formatAuthnRequest(request, direction, validSignature));
    }

    public void logAuthnResponse(Response samlResponse, Direction direction, Boolean validSignature) {
        LOG.info(formatter.formatAuthnResponse(samlResponse, direction, validSignature));
    }

}
