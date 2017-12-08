package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.common.base.Optional;
import javax.inject.Inject;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventsink.EventDetails;
import uk.gov.ida.eventsink.EventSinkMessageSender;

import java.net.URI;

import static uk.gov.ida.common.SessionId.NO_SESSION_CONTEXT_IN_ERROR;
import static uk.gov.ida.eventsink.EventDetailsKey.downstream_uri;

public class ExceptionAuditor {
    private final EventSinkMessageSender eventSinkMessageSender;

    @Inject
    public ExceptionAuditor(EventSinkMessageSender eventSinkMessageSender) {
        this.eventSinkMessageSender = eventSinkMessageSender;
    }

    public boolean auditException(ApplicationException exception, Optional<SessionId> sessionId) {
        boolean isAudited = exception.isAudited();

        if (!isAudited && exception.requiresAuditing()) {
            EventDetails eventDetails = new EventDetails(
                    downstream_uri,
                    exception.getUri().or(URI.create("uri-not-present")).toASCIIString());

            if (sessionId.isPresent()) {
                eventSinkMessageSender.audit(exception, exception.getErrorId(), sessionId.get(), eventDetails);
            } else {
                eventSinkMessageSender.audit(exception, exception.getErrorId(), NO_SESSION_CONTEXT_IN_ERROR, eventDetails);
            }

            isAudited = true;
        }

        return isAudited;
    }
}
