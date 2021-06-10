package uk.gov.ida.hub.policy.domain.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.exception.PolicyExceptionMapper;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.UUID;

import static uk.gov.ida.common.ErrorStatusDto.createUnauditedErrorStatus;
import static uk.gov.ida.common.ExceptionType.SESSION_NOT_FOUND;

public class SessionNotFoundExceptionMapper extends PolicyExceptionMapper<SessionNotFoundException> {

    private static final Logger LOG = LoggerFactory.getLogger(SessionNotFoundExceptionMapper.class);

    private final HubEventLogger eventLogger;

    @Inject
    public SessionNotFoundExceptionMapper(HubEventLogger eventLogger) {
        super();
        this.eventLogger = eventLogger;
    }

    @Override
    public Response handleException(SessionNotFoundException exception) {
        UUID errorId = UUID.randomUUID();
        LOG.info(MessageFormat.format("{0} - Exception while processing request.", errorId), exception);

        eventLogger.logErrorEvent(errorId, getSessionId().orElse(SessionId.SESSION_ID_DOES_NOT_EXIST_YET), exception.getMessage());

        ErrorStatusDto entity = createUnauditedErrorStatus(errorId, SESSION_NOT_FOUND);
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(entity).build();
    }

    protected SessionId createSessionId(String parameter) {
        return new SessionId(parameter);
    }
}
