package uk.gov.ida.hub.samlproxy.exceptions;

import java.util.Optional;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.shared.eventsink.EventDetails;
import uk.gov.ida.hub.shared.eventsink.EventSinkMessageSender;
import uk.gov.ida.exceptions.ApplicationException;

import javax.inject.Inject;
import java.net.URI;

import static uk.gov.ida.eventemitter.EventDetailsKey.downstream_uri;

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
                    exception.getUri().orElse(URI.create("uri-not-present")).toASCIIString());

            eventSinkMessageSender.audit(
                    exception,
                    exception.getErrorId(),
                    sessionId.orElse(SessionId.NO_SESSION_CONTEXT_IN_ERROR),
                    eventDetails);

            isAudited = true;
        }

        return isAudited;
    }
}
