package uk.gov.ida.hub.policy.domain.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.policy.exception.PolicyExceptionMapper;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.UUID;

import static uk.gov.ida.common.ErrorStatusDto.createAuditedErrorStatus;
import static uk.gov.ida.common.ExceptionType.DUPLICATE_SESSION;

public class SessionAlreadyExistingExceptionMapper extends PolicyExceptionMapper<SessionAlreadyExistingException> {

    private static final Logger LOG = LoggerFactory.getLogger(SessionAlreadyExistingExceptionMapper.class);

    private final HubEventLogger eventLogger;

    @Inject
    public SessionAlreadyExistingExceptionMapper(HubEventLogger eventLogger) {
        super();
        this.eventLogger = eventLogger;
    }

    @Override
    public Response handleException(SessionAlreadyExistingException exception) {
        UUID errorId = UUID.randomUUID();
        LOG.error(MessageFormat.format("{0} - Exception while processing request.", errorId), exception);

        eventLogger.logErrorEvent(errorId, exception.getSessionId(), exception.getMessage());

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(createAuditedErrorStatus(errorId, DUPLICATE_SESSION))
                .build();
    }
}
