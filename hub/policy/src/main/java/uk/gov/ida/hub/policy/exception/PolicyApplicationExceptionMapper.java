package uk.gov.ida.hub.policy.exception;

import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static uk.gov.ida.common.ErrorStatusDto.createAuditedErrorStatus;
import static uk.gov.ida.common.ErrorStatusDto.createUnauditedErrorStatus;
import static uk.gov.ida.hub.policy.domain.SessionId.NO_SESSION_CONTEXT_IN_ERROR;

public class PolicyApplicationExceptionMapper extends PolicyExceptionMapper<ApplicationException> {
    private final HubEventLogger eventLogger;
    private final LevelLogger levelLogger;

    @Inject
    public PolicyApplicationExceptionMapper(final HubEventLogger eventLogger) {
        super();
        this.eventLogger = eventLogger;
        this.levelLogger = new LevelLoggerFactory<PolicyApplicationExceptionMapper>().createLevelLogger(PolicyApplicationExceptionMapper.class);
    }

    protected Response createResponse(
            final ApplicationException exception,
            final boolean isAudited) {

        final ErrorStatusDto errorStatus;
        if (isAudited) {
            errorStatus = createAuditedErrorStatus(
                    exception.getErrorId(),
                    exception.getExceptionType()
            );
        } else {
            errorStatus = createUnauditedErrorStatus(
                    exception.getErrorId(),
                    exception.getExceptionType()
            );
        }
        return Response.status(BAD_REQUEST).entity(errorStatus).build();
    }

    @Override
    protected Response handleException(final ApplicationException exception) {
        levelLogger.log(exception.getExceptionType().getLevel(), exception, exception.getErrorId());

        boolean isAudited = auditException(exception);

        return createResponse(exception, isAudited);
    }

    private boolean auditException(final ApplicationException exception) {
        boolean isAudited = exception.isAudited();

        if (!isAudited && exception.requiresAuditing()) {
            eventLogger.logErrorEvent(
                exception.getErrorId(),
                getSessionId().orElse(NO_SESSION_CONTEXT_IN_ERROR),
                exception.getMessage(),
                exception.getUri().or(URI.create("uri-not-present")).toASCIIString());
            isAudited = true;
        }

        return isAudited;
    }
}
