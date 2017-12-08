package uk.gov.ida.hub.policy.exception;

import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.eventsink.EventDetails;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.facade.EventSinkMessageSenderFacade;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static uk.gov.ida.common.ErrorStatusDto.createAuditedErrorStatus;
import static uk.gov.ida.common.ErrorStatusDto.createUnauditedErrorStatus;
import static uk.gov.ida.eventsink.EventDetailsKey.downstream_uri;
import static uk.gov.ida.hub.policy.domain.SessionId.NO_SESSION_CONTEXT_IN_ERROR;

public class PolicyApplicationExceptionMapper extends PolicyExceptionMapper<ApplicationException> {
    private final EventSinkMessageSenderFacade eventSinkMessageSenderFacade;
    private final LevelLogger levelLogger;

    @Inject
    public PolicyApplicationExceptionMapper(
            final EventSinkMessageSenderFacade eventSinkMessageSenderFacade) {
        super();
        this.eventSinkMessageSenderFacade = eventSinkMessageSenderFacade;
        levelLogger = new LevelLoggerFactory<PolicyApplicationExceptionMapper>().createLevelLogger(PolicyApplicationExceptionMapper.class);
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
            EventDetails eventDetails = new EventDetails(
                    downstream_uri,
                    exception.getUri().or(URI.create("uri-not-present")).toASCIIString());

            if (getSessionId().isPresent()) {
                eventSinkMessageSenderFacade.audit(exception, exception.getErrorId(), getSessionId().get(), eventDetails);
            } else {
                eventSinkMessageSenderFacade.audit(exception, exception.getErrorId(), NO_SESSION_CONTEXT_IN_ERROR,
                        eventDetails
                );
            }

            isAudited = true;
        }

        return isAudited;
    }
}
