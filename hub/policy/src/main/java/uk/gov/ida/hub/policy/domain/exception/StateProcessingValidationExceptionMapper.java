package uk.gov.ida.hub.policy.domain.exception;

import com.google.common.collect.ImmutableMap;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.exception.PolicyExceptionMapper;
import uk.gov.ida.hub.policy.proxy.EventSinkProxy;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

import static uk.gov.ida.common.ErrorStatusDto.createUnauditedErrorStatus;
import static uk.gov.ida.common.ExceptionType.STATE_PROCESSING_VALIDATION;
import static uk.gov.ida.eventsink.EventDetailsKey.error_id;
import static uk.gov.ida.eventsink.EventDetailsKey.message;

public class StateProcessingValidationExceptionMapper extends PolicyExceptionMapper<StateProcessingValidationException> {

    private static final LevelLogger LOG = new LevelLoggerFactory<StateProcessingValidationExceptionMapper>().createLevelLogger(StateProcessingValidationExceptionMapper.class);

    private final ServiceInfoConfiguration serviceInfo;
    private final EventSinkProxy eventSinkProxy;

    @Inject
    public StateProcessingValidationExceptionMapper(
            ServiceInfoConfiguration serviceInfo,
            EventSinkProxy eventSinkProxy) {

        super();
        this.serviceInfo = serviceInfo;
        this.eventSinkProxy = eventSinkProxy;
    }

    @Override
    public Response handleException(StateProcessingValidationException exception) {
        UUID errorId = UUID.randomUUID();

        LOG.log(exception.getLevel(), exception, errorId);

        Map<EventDetailsKey, String> details = ImmutableMap.of(
                message, exception.getMessage(),
                error_id, errorId.toString());

        EventSinkHubEvent event = new EventSinkHubEvent(
                serviceInfo,
                getSessionId().get(),
                EventSinkHubEventConstants.EventTypes.ERROR_EVENT,
                details);

        eventSinkProxy.logHubEvent(event);

        ErrorStatusDto entity = createUnauditedErrorStatus(errorId, STATE_PROCESSING_VALIDATION);
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(entity).build();
    }

    protected SessionId createSessionId(String parameter) {
        return new SessionId(parameter);
    }
}
