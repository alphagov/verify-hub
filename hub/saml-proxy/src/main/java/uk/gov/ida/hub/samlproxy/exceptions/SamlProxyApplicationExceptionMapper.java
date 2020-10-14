package uk.gov.ida.hub.samlproxy.exceptions;

import javax.inject.Provider;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class SamlProxyApplicationExceptionMapper extends AbstractContextExceptionMapper<ApplicationException> {
    private final LevelLogger levelLogger;
    private final ExceptionAuditor exceptionAuditor;

    @Inject
    public SamlProxyApplicationExceptionMapper(
            final Provider<HttpServletRequest> servletRequestProvider,
            final ExceptionAuditor exceptionAuditor,
            LevelLoggerFactory<SamlProxyApplicationExceptionMapper> levelLoggerFactory) {
        super(servletRequestProvider);
        this.exceptionAuditor = exceptionAuditor;
        this.levelLogger = levelLoggerFactory.createLevelLogger(SamlProxyApplicationExceptionMapper.class);
    }

    @Override
    protected Response handleException(final ApplicationException exception) {
        exceptionAuditor.auditException(exception, getSessionId());

        UUID errorId = exception.getErrorId();
        levelLogger.log(exception.getExceptionType().getLevel(), exception, errorId);

        return Response.serverError()
                .entity(ErrorStatusDto.createAuditedErrorStatus(errorId, exception.getExceptionType(), exception.getMessage()))
                .build();
    }
}
