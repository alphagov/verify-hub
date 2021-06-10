package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.inject.Provider;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.shared.eventsink.EventSinkMessageSender;
import uk.gov.ida.saml.hub.exception.SamlDuplicateRequestIdException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static uk.gov.ida.common.ExceptionType.INVALID_SAML_DUPLICATE_REQUEST_ID;

public class SamlProxyDuplicateRequestExceptionMapper extends AbstractContextExceptionMapper<SamlDuplicateRequestIdException> {

    private EventSinkMessageSender eventSinkMessageSender;
    private final LevelLogger levelLogger;

    @Inject
    public SamlProxyDuplicateRequestExceptionMapper(
            Provider<HttpServletRequest> contextProvider,
            EventSinkMessageSender eventSinkMessageSender,
            LevelLoggerFactory<SamlProxyDuplicateRequestExceptionMapper> levelLoggerFactory) {
        super(contextProvider);
        this.eventSinkMessageSender = eventSinkMessageSender;
        this.levelLogger = levelLoggerFactory.createLevelLogger(SamlProxyDuplicateRequestExceptionMapper.class);
    }

    @Override
    protected Response handleException(SamlDuplicateRequestIdException exception) {
        UUID errorId = UUID.randomUUID();

        eventSinkMessageSender.audit(exception, errorId, getSessionId().orElse(SessionId.NO_SESSION_CONTEXT_IN_ERROR));
        levelLogger.log(ExceptionType.INVALID_SAML_DUPLICATE_REQUEST_ID.getLevel(), exception, errorId);

        return Response.status(BAD_REQUEST)
                .entity(ErrorStatusDto.createAuditedErrorStatus(errorId, INVALID_SAML_DUPLICATE_REQUEST_ID))
                .build();
    }
}
