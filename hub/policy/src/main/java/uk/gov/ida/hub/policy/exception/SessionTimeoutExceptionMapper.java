package uk.gov.ida.hub.policy.exception;

import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.text.MessageFormat;
import java.util.UUID;

import static uk.gov.ida.common.ExceptionType.SESSION_TIMEOUT;

@javax.ws.rs.ext.Provider
public class SessionTimeoutExceptionMapper extends PolicyExceptionMapper<SessionTimeoutException> {

    private static final Logger LOG = LoggerFactory.getLogger(SessionTimeoutExceptionMapper.class);

    private final HubEventLogger hubEventLogger;

    @Inject
    public SessionTimeoutExceptionMapper(
            Provider<UriInfo> uriInfoProvider,
            Provider<HttpServletRequest> servletRequestProvider,
            HubEventLogger hubEventLogger) {
        super(uriInfoProvider, servletRequestProvider);
        this.hubEventLogger = hubEventLogger;
    }

    @Override
    public Response handleException(SessionTimeoutException exception) {
        UUID errorId = UUID.randomUUID();
        hubEventLogger.logSessionTimeoutEvent(exception.getSessionId(), exception.getSessionExpiryTimestamp(), exception.getTransactionEntityId(), exception.getRequestId());
        LOG.info(MessageFormat.format("{0} - Timeout while processing request with session id: {1}.", errorId, exception.getSessionId().getSessionId()), exception);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorStatusDto.createAuditedErrorStatus(errorId, SESSION_TIMEOUT, exception.getMessage()))
                .build();
    }
}
