package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.inject.Provider;
import org.slf4j.event.Level;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventsink.EventSinkMessageSender;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Optional;
import java.util.UUID;

public class NoKeyConfiguredForEntityExceptionMapper implements ExceptionMapper<NoKeyConfiguredForEntityException> {

    private final LevelLogger levelLogger;
    private Provider<HttpServletRequest> context;
    private final EventSinkMessageSender eventSinkMessageSender;

    @Inject
    public NoKeyConfiguredForEntityExceptionMapper(
            final Provider<HttpServletRequest> context,
            final LevelLoggerFactory<NoKeyConfiguredForEntityExceptionMapper> levelLoggerFactory,
            final EventSinkMessageSender eventSinkMessageSender) {
        this.context = context;
        this.eventSinkMessageSender = eventSinkMessageSender;
        this.levelLogger = levelLoggerFactory.createLevelLogger(NoKeyConfiguredForEntityExceptionMapper.class);
    }

    @Override
    public Response toResponse(NoKeyConfiguredForEntityException exception) {
        levelLogger.log(Level.ERROR, exception);
        final Optional<SessionId> sessionId = Optional
                .ofNullable(context.get().getParameter(Urls.SharedUrls.SESSION_ID_PARAM))
                .map(SessionId::new);
        eventSinkMessageSender.audit(exception, UUID.randomUUID(), sessionId.orElse(SessionId.NO_SESSION_CONTEXT_IN_ERROR));
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
