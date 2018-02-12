package uk.gov.ida.hub.policy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.shared.utils.logging.LogFormatter;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class IdpDisabledExceptionMapper extends PolicyExceptionMapper<IdpDisabledException> {

    private static final Logger LOG = LoggerFactory.getLogger(IdpDisabledExceptionMapper.class);

    private final HubEventLogger eventLogger;

    @Inject
    public IdpDisabledExceptionMapper(HubEventLogger eventLogger) {

        super();
        this.eventLogger = eventLogger;
    }

    @Override
    public Response handleException(IdpDisabledException exception) {
        UUID errorId = UUID.randomUUID();
        LOG.error(LogFormatter.formatLog(errorId, exception.getMessage()), exception);

        eventLogger.logErrorEvent(errorId, exception.getEntityId(), getSessionId().or(SessionId.SESSION_ID_DOES_NOT_EXIST_YET));

        return Response.status(Response.Status.FORBIDDEN).entity(ErrorStatusDto.createAuditedErrorStatus(errorId, ExceptionType
                .IDP_DISABLED)).build();
    }
}
