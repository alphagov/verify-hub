package uk.gov.ida.hub.samlproxy.exceptions;

import javax.inject.Inject;

import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

@javax.ws.rs.ext.Provider
public class SamlProxyExceptionMapper extends AbstractContextExceptionMapper<Exception> {

    private final LevelLogger levelLogger;

    @Inject
    public SamlProxyExceptionMapper(
            final com.google.inject.Provider<HttpServletRequest> servletRequestProvider,
            LevelLoggerFactory<SamlProxyExceptionMapper> levelLoggerFactory) {
        super(servletRequestProvider);
        this.levelLogger = levelLoggerFactory.createLevelLogger(SamlProxyExceptionMapper.class);
    }

    @Override
    protected Response handleException(Exception exception) {
        UUID errorId = UUID.randomUUID();
        levelLogger.log(ExceptionType.UNKNOWN.getLevel(), exception, errorId);
        return Response.serverError()
                .entity(ErrorStatusDto.createAuditedErrorStatus(errorId, ExceptionType.UNKNOWN))
                .build();
    }
}
