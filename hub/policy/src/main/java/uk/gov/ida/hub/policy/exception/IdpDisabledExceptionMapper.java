package uk.gov.ida.hub.policy.exception;

import com.google.inject.servlet.RequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.shared.utils.logging.LogFormatter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

@Provider
@RequestScoped
public class IdpDisabledExceptionMapper extends PolicyExceptionMapper<IdpDisabledException> {

    private static final Logger LOG = LoggerFactory.getLogger(IdpDisabledExceptionMapper.class);

    private final HubEventLogger eventLogger;

    @Inject
    public IdpDisabledExceptionMapper(@Context UriInfo uriInfo, @Context HttpServletRequest request, HubEventLogger eventLogger) {
        super(uriInfo, request);
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
