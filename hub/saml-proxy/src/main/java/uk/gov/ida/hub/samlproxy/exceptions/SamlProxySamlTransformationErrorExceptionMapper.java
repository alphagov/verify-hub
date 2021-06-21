package uk.gov.ida.hub.samlproxy.exceptions;

import javax.inject.Inject;
import com.google.inject.Provider;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.shared.eventsink.EventSinkMessageSender;
import uk.gov.ida.saml.hub.exception.SamlRequestTooOldException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class SamlProxySamlTransformationErrorExceptionMapper extends AbstractContextExceptionMapper<SamlTransformationErrorException> {

    private EventSinkMessageSender eventSinkMessageSender;
    private final LevelLogger levelLogger;

    @Inject
    public SamlProxySamlTransformationErrorExceptionMapper(
            Provider<HttpServletRequest> contextProvider,
            EventSinkMessageSender eventSinkMessageSender,
            LevelLoggerFactory<SamlProxySamlTransformationErrorExceptionMapper> levelLoggerFactory) {
        super(contextProvider);
        this.eventSinkMessageSender = eventSinkMessageSender;
        this.levelLogger = levelLoggerFactory.createLevelLogger(SamlProxySamlTransformationErrorExceptionMapper.class);
    }

    @Override
    protected Response handleException(SamlTransformationErrorException exception) {
        UUID errorId = UUID.randomUUID();

        eventSinkMessageSender.audit(exception, errorId, getSessionId().orElse(SessionId.NO_SESSION_CONTEXT_IN_ERROR));
        levelLogger.log(exception.getLogLevel(), exception, errorId);

        ErrorStatusDto auditedErrorStatus = ErrorStatusDto.createAuditedErrorStatus(errorId, getExceptionTypeForSamlException(exception));

        return Response.status(Response.Status.BAD_REQUEST).entity(auditedErrorStatus).build();
    }

    private ExceptionType getExceptionTypeForSamlException(SamlTransformationErrorException exception) {
        if (exception instanceof SamlRequestTooOldException) {
            return ExceptionType.INVALID_SAML_REQUEST_TOO_OLD;
        } else {
            return ExceptionType.INVALID_SAML;
        }
    }
}
