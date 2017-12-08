package uk.gov.ida.hub.policy.domain.exception;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.exception.PolicyExceptionMapper;
import uk.gov.ida.hub.policy.proxy.EventSinkProxy;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.Map;
import java.util.UUID;

import static uk.gov.ida.common.ErrorStatusDto.createAuditedErrorStatus;
import static uk.gov.ida.common.ExceptionType.DUPLICATE_SESSION;
import static uk.gov.ida.eventsink.EventDetailsKey.error_id;
import static uk.gov.ida.eventsink.EventDetailsKey.message;

public class SessionAlreadyExistingExceptionMapper extends PolicyExceptionMapper<SessionAlreadyExistingException> {

    private static final Logger LOG = LoggerFactory.getLogger(SessionAlreadyExistingExceptionMapper.class);

    private final ServiceInfoConfiguration serviceInfo;
    private final EventSinkProxy eventSinkProxy;

    @Inject
    public SessionAlreadyExistingExceptionMapper(ServiceInfoConfiguration serviceInfo, EventSinkProxy eventSinkProxy) {
        super();
        this.serviceInfo = serviceInfo;
        this.eventSinkProxy = eventSinkProxy;
    }

    @Override
    public Response handleException(SessionAlreadyExistingException exception) {
        UUID errorId = UUID.randomUUID();
        LOG.error(MessageFormat.format("{0} - Exception while processing request.", errorId), exception);

        Map<EventDetailsKey, String> details = ImmutableMap.of(
                message, exception.getMessage(),
                error_id, errorId.toString());

        eventSinkProxy.logHubEvent(new EventSinkHubEvent(
                serviceInfo,
                exception.getSessionId(),
                EventSinkHubEventConstants.EventTypes.ERROR_EVENT,
                details));

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(createAuditedErrorStatus(errorId, DUPLICATE_SESSION))
                .build();
    }
}
