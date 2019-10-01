package uk.gov.ida.hub.policy.domain.exception;

import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.exception.PolicyExceptionMapper;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class SessionCreationFailureExceptionMapper extends PolicyExceptionMapper<SessionCreationFailureException> {

    private final LevelLogger levelLogger;

    private final HubEventLogger eventLogger;

    @Inject
    public SessionCreationFailureExceptionMapper(HubEventLogger eventLogger) {
        levelLogger = new LevelLoggerFactory<SessionCreationFailureExceptionMapper>().createLevelLogger(SessionCreationFailureExceptionMapper.class);
        this.eventLogger = eventLogger;
    }

    @Override
    public Response handleException(SessionCreationFailureException exception) {
        UUID errorId = UUID.randomUUID();
        levelLogger.log(exception.getLogLevel(), exception);

        eventLogger.logErrorEvent(errorId, getSessionId().orElse(SessionId.SESSION_ID_DOES_NOT_EXIST_YET), exception.getMessage());

        final ErrorStatusDto entity = ErrorStatusDto.createAuditedErrorStatus(errorId, exception.getExceptionType(), exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(entity).build();
    }
}
