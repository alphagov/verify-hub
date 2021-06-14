package uk.gov.ida.hub.samlproxy.exceptions;

import javax.inject.Inject;
import com.google.inject.Provider;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class SamlProxyExceptionMapper extends AbstractContextExceptionMapper<Exception> {

    private final LevelLogger levelLogger;

    @Inject
    public SamlProxyExceptionMapper(
            Provider<HttpServletRequest> contextProvider,
            LevelLoggerFactory<SamlProxyExceptionMapper> levelLoggerFactory) {
        super(contextProvider);
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
