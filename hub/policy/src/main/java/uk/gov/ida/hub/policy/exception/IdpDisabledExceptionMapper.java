package uk.gov.ida.hub.policy.exception;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.shared.utils.logging.LogFormatter;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

import static uk.gov.ida.eventsink.EventDetailsKey.error_id;
import static uk.gov.ida.eventsink.EventDetailsKey.idp_entity_id;

public class IdpDisabledExceptionMapper extends PolicyExceptionMapper<IdpDisabledException> {

    private static final Logger LOG = LoggerFactory.getLogger(IdpDisabledExceptionMapper.class);

    private final ServiceInfoConfiguration serviceInfo;
    private final EventSinkProxy eventSinkProxy;

    @Inject
    public IdpDisabledExceptionMapper(
            ServiceInfoConfiguration serviceInfo,
            EventSinkProxy eventSinkProxy) {

        super();
        this.serviceInfo = serviceInfo;
        this.eventSinkProxy = eventSinkProxy;
    }

    @Override
    public Response handleException(IdpDisabledException exception) {
        UUID errorId = UUID.randomUUID();
        LOG.error(LogFormatter.formatLog(errorId, exception.getMessage()), exception);

        Map<EventDetailsKey, String> details = ImmutableMap.of(
                idp_entity_id, exception.getEntityId(),
                error_id, errorId.toString());
        EventSinkHubEvent event = new EventSinkHubEvent(serviceInfo, getSessionId().get(), EventSinkHubEventConstants.EventTypes.ERROR_EVENT, details);
        eventSinkProxy.logHubEvent(event);

        return Response.status(Response.Status.FORBIDDEN).entity(ErrorStatusDto.createAuditedErrorStatus(errorId, ExceptionType
                .IDP_DISABLED)).build();
    }
}
