package uk.gov.ida.hub.policy.domain.exception;

import com.google.common.collect.ImmutableMap;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.exception.PolicyExceptionMapper;
import uk.gov.ida.hub.policy.proxy.EventSinkProxy;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

import static uk.gov.ida.eventsink.EventDetailsKey.error_id;
import static uk.gov.ida.eventsink.EventDetailsKey.message;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.EventTypes.ERROR_EVENT;

public class SessionCreationFailureExceptionMapper extends PolicyExceptionMapper<SessionCreationFailureException> {

    private final ServiceInfoConfiguration serviceInfo;
    private final EventSinkProxy eventSinkProxy;
    private final LevelLogger levelLogger;

    @Inject
    public SessionCreationFailureExceptionMapper(
            ServiceInfoConfiguration serviceInfo,
            EventSinkProxy eventSinkProxy) {

        this.serviceInfo = serviceInfo;
        this.eventSinkProxy = eventSinkProxy;
        levelLogger = new LevelLoggerFactory<SessionCreationFailureExceptionMapper>().createLevelLogger(SessionCreationFailureExceptionMapper.class);
    }

    @Override
    public Response handleException(SessionCreationFailureException exception) {
        UUID errorId = UUID.randomUUID();
        levelLogger.log(exception.getLogLevel(), exception);
        Map<EventDetailsKey, String> details = ImmutableMap.of(
                error_id, errorId.toString(),
                message, exception.getMessage());
        EventSinkHubEvent event = new EventSinkHubEvent(serviceInfo, getSessionId().get(), ERROR_EVENT, details);
        eventSinkProxy.logHubEvent(event);

        final ErrorStatusDto entity = ErrorStatusDto.createAuditedErrorStatus(errorId, exception.getExceptionType(), exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(entity).build();
    }
}
