package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.inject.Provider;
import org.slf4j.event.Level;

import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.shared.eventsink.EventSinkMessageSender;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class NoKeyConfiguredForEntityExceptionMapper extends AbstractContextExceptionMapper<NoKeyConfiguredForEntityException> {

    private final LevelLogger levelLogger;
    private final EventSinkMessageSender eventSinkMessageSender;

    @Inject
    public NoKeyConfiguredForEntityExceptionMapper(
            final Provider<HttpServletRequest> context,
            final LevelLoggerFactory<NoKeyConfiguredForEntityExceptionMapper> levelLoggerFactory,
            final EventSinkMessageSender eventSinkMessageSender) {
        super(context);
        this.eventSinkMessageSender = eventSinkMessageSender;
        this.levelLogger = levelLoggerFactory.createLevelLogger(NoKeyConfiguredForEntityExceptionMapper.class);
    }

    @Override
    public Response handleException(NoKeyConfiguredForEntityException exception) {
        UUID errorId = UUID.randomUUID();
        levelLogger.log(Level.WARN, exception);
        eventSinkMessageSender.audit(exception, errorId, getSessionId().orElse(SessionId.NO_SESSION_CONTEXT_IN_ERROR));

        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ErrorStatusDto.createAuditedErrorStatus(errorId, ExceptionType.NO_KEY_CONFIGURED_FOR_ENTITY))
            .build();
    }
}
