package uk.gov.ida.hub.samlengine.exceptions;

import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.hub.exception.SamlDuplicateRequestIdException;
import uk.gov.ida.saml.hub.exception.SamlRequestTooOldException;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.saml.security.exception.SamlFailedToDecryptException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.UUID;

import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

public class SamlEngineExceptionMapper implements ExceptionMapper<Exception> {

    private static final boolean HAS_NOT_BEEN_AUDITED_YET = false;

    private final LevelLogger<SamlEngineExceptionMapper> levelLogger;

    @Inject
    public SamlEngineExceptionMapper(final LevelLoggerFactory<SamlEngineExceptionMapper> levelLoggerFactory) {
        this.levelLogger = levelLoggerFactory.createLevelLogger(SamlEngineExceptionMapper.class);
    }

    @Override
    public Response toResponse(Exception exception) {
        final UUID errorId = UUID.randomUUID();
        Response.ResponseBuilder response = Response.status(Response.Status.BAD_REQUEST);

        // when creating a new exception here we want to create an unaudited application exception - this is to ensure
        // that the exception is audited by the calling application, as saml-engine no longer talks directly to
        // event sink

        if(exception instanceof ApplicationException) {
            ApplicationException applicationException = (ApplicationException)exception;
            response.entity(logAndGetErrorStatusDto(applicationException.getExceptionType().getLevel(), applicationException.getExceptionType(), applicationException, applicationException.getErrorId(), applicationException.isAudited()));
        } else if(exception instanceof SamlContextException) {
            SamlContextException contextException = (SamlContextException) exception;
            response.entity(logAndGetErrorStatusDto(contextException.getLogLevel(), contextException.getExceptionType(), exception, errorId, HAS_NOT_BEEN_AUDITED_YET));
        } else if(exception instanceof SamlFailedToDecryptException) {
            response.entity(logAndGetErrorStatusDto(((SamlFailedToDecryptException) exception).getLogLevel(), ExceptionType.INVALID_SAML_FAILED_TO_DECRYPT, exception, errorId, HAS_NOT_BEEN_AUDITED_YET));
        } else if(exception instanceof SamlDuplicateRequestIdException) {
            response.entity(logAndGetErrorStatusDto(((SamlDuplicateRequestIdException) exception).getLogLevel(), ExceptionType.INVALID_SAML_DUPLICATE_REQUEST_ID, exception, errorId, HAS_NOT_BEEN_AUDITED_YET));
        } else if(exception instanceof SamlRequestTooOldException) {
            response.entity(logAndGetErrorStatusDto(((SamlTransformationErrorException) exception).getLogLevel(), ExceptionType.INVALID_SAML_REQUEST_TOO_OLD, exception, errorId, HAS_NOT_BEEN_AUDITED_YET));
        } else if(exception instanceof SamlTransformationErrorException) {
            response.entity(logAndGetErrorStatusDto(((SamlTransformationErrorException) exception).getLogLevel(), ExceptionType.INVALID_SAML, exception, errorId, HAS_NOT_BEEN_AUDITED_YET));
        } else if(exception instanceof UnableToGenerateSamlException) {
            response.entity(logAndGetErrorStatusDto(((UnableToGenerateSamlException) exception).getLogLevel(), ExceptionType.INVALID_INPUT, exception, errorId, HAS_NOT_BEEN_AUDITED_YET));
        } else if (exception instanceof NoKeyConfiguredForEntityException) {
            response.entity(logAndGetErrorStatusDto(ERROR, ExceptionType.NO_KEY_CONFIGURED_FOR_ENTITY, exception, errorId, HAS_NOT_BEEN_AUDITED_YET));
        } else {
            levelLogger.log(WARN, exception, errorId);
        }

        return response.build();
    }

    private ErrorStatusDto logAndGetErrorStatusDto(Level logLevel, ExceptionType exceptionType, Exception exception, UUID errorId, boolean hasAlreadyBeenAudited) {
        levelLogger.log(logLevel, exception, errorId);
        if(hasAlreadyBeenAudited) {
            return ErrorStatusDto.createAuditedErrorStatus(errorId, exceptionType, exception.getMessage());
        } else {
            return ErrorStatusDto.createUnauditedErrorStatus(errorId, exceptionType, exception.getMessage());
        }
    }

}
