package uk.gov.ida.hub.policy.exception;

import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.shared.utils.logging.LogFormatter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

@javax.ws.rs.ext.Provider
public class IdpDisabledExceptionMapper extends PolicyExceptionMapper<IdpDisabledException> {

    private static final Logger LOG = LoggerFactory.getLogger(IdpDisabledExceptionMapper.class);

    private final HubEventLogger eventLogger;

    @Inject
    public IdpDisabledExceptionMapper(
            Provider<UriInfo> uriInfoProvider,
            Provider<HttpServletRequest> servletRequestProvider,
            HubEventLogger eventLogger) {
        super(uriInfoProvider, servletRequestProvider);
        this.eventLogger = eventLogger;
    }

    @Override
    public Response handleException(IdpDisabledException exception) {
        UUID errorId = UUID.randomUUID();
        LOG.error(LogFormatter.formatLog(errorId, exception.getMessage()), exception);

        eventLogger.logErrorEvent(errorId, exception.getEntityId(), getSessionId().orElse(SessionId.SESSION_ID_DOES_NOT_EXIST_YET));

        return Response.status(Response.Status.FORBIDDEN).entity(ErrorStatusDto.createAuditedErrorStatus(errorId, ExceptionType
                .IDP_DISABLED)).build();
    }
}
