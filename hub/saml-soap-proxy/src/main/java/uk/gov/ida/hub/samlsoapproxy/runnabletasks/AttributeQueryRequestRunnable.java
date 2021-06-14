package uk.gov.ida.hub.samlsoapproxy.runnabletasks;

import com.codahale.metrics.Counter;
import org.glassfish.jersey.internal.util.Base64;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.eventemitter.EventDetailsKey;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEvent;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;
import uk.gov.ida.hub.samlsoapproxy.annotations.MatchingServiceRequestExecutorBacklog;
import uk.gov.ida.hub.samlsoapproxy.domain.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlsoapproxy.domain.TimeoutEvaluator;
import uk.gov.ida.hub.samlsoapproxy.exceptions.AttributeQueryTimeoutException;
import uk.gov.ida.hub.samlsoapproxy.exceptions.MatchingServiceRequestExceptionErrorMessageMapper;
import uk.gov.ida.hub.samlsoapproxy.proxy.HubMatchingServiceResponseReceiverProxy;
import uk.gov.ida.shared.utils.logging.LogFormatter;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.text.MessageFormat.format;
import static uk.gov.ida.eventemitter.EventDetailsKey.error_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.idp_entity_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.message;

public class AttributeQueryRequestRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AttributeQueryRequestRunnable.class);

    private final SessionId sessionId;
    private final AttributeQueryContainerDto attributeQueryContainerDto;
    private final ExecuteAttributeQueryRequest executeAttributeQueryRequest;
    private final Counter counter;
    private final TimeoutEvaluator timeoutEvaluator;
    private final HubMatchingServiceResponseReceiverProxy hubMatchingServiceResponseReceiverProxy;
    private final ServiceInfoConfiguration serviceInfo;
    private final EventSinkProxy eventSinkProxy;
    private final EventEmitter eventEmitter;

    public AttributeQueryRequestRunnable(SessionId sessionId,
                                         AttributeQueryContainerDto attributeQueryContainerDto,
                                         ExecuteAttributeQueryRequest executeAttributeQueryRequest,
                                         @MatchingServiceRequestExecutorBacklog Counter counter,
                                         TimeoutEvaluator timeoutEvaluator,
                                         HubMatchingServiceResponseReceiverProxy hubMatchingServiceResponseReceiverProxy,
                                         ServiceInfoConfiguration serviceInfo,
                                         EventSinkProxy eventSinkProxy,
                                         EventEmitter eventEmitter) {
        this.counter = counter;
        this.sessionId = sessionId;
        this.attributeQueryContainerDto = attributeQueryContainerDto;
        this.executeAttributeQueryRequest = executeAttributeQueryRequest;
        this.timeoutEvaluator = timeoutEvaluator;
        this.hubMatchingServiceResponseReceiverProxy = hubMatchingServiceResponseReceiverProxy;
        this.serviceInfo = serviceInfo;
        this.eventSinkProxy = eventSinkProxy;
        this.eventEmitter = eventEmitter;
        this.counter.inc();
    }

    @Override
    public void run() {
        counter.dec();

        // see https://github.com/google/guice/wiki/CustomScopes for details on this work

        addSessionIdToLoggingContext(sessionId);

        try {
            timeoutEvaluator.hasAttributeQueryTimedOut(attributeQueryContainerDto);
        } catch (AttributeQueryTimeoutException e) {
            auditAndLogTimeoutException(sessionId, attributeQueryContainerDto, e, "Matching service attribute timed out before even being sent.");
            return;
        }
        try {
            Element response = executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto);
            timeoutEvaluator.hasAttributeQueryTimedOut(attributeQueryContainerDto);
            final String base64EncodedSamlResponse = Base64.encodeAsString(XmlUtils.writeToString(response).getBytes());
            hubMatchingServiceResponseReceiverProxy.notifyHubOfAResponseFromMatchingService(
                    sessionId,
                    base64EncodedSamlResponse);
        } catch (AttributeQueryTimeoutException e) {
            auditAndLogTimeoutException(sessionId, attributeQueryContainerDto, e, "Matching service attribute query has timed out, therefore not sending failure notification to saml engine.");
        } catch (Exception e) {
            logAndAuditMessageError(e, attributeQueryContainerDto, sessionId);
            checkTimeoutAndForwardErrorResponse(sessionId, attributeQueryContainerDto);
        }
    }

    private void addSessionIdToLoggingContext(final SessionId sessionId) {
        MDC.put("SessionId", sessionId);
    }

    private void auditAndLogTimeoutException(SessionId sessionId, AttributeQueryContainerDto attributeQueryContainerDto, AttributeQueryTimeoutException exception, String message) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(idp_entity_id, attributeQueryContainerDto.getIssuer());
        String errorId = UUID.randomUUID().toString();
        details.put(error_id, errorId);
        details.put(EventDetailsKey.message, message);
        EventSinkHubEvent hubEvent = new EventSinkHubEvent(
                serviceInfo,
                sessionId,
                EventSinkHubEventConstants.EventTypes.ERROR_EVENT,
                details);
        eventSinkProxy.logHubEvent(hubEvent);
        eventEmitter.record(hubEvent);

        LOG.warn(format(message + " It has been Audited with error id: {0}.", errorId), exception);
    }

    private void checkTimeoutAndForwardErrorResponse(SessionId sessionId, AttributeQueryContainerDto attributeQueryContainerDto) {
        try {
            timeoutEvaluator.hasAttributeQueryTimedOut(attributeQueryContainerDto);
            hubMatchingServiceResponseReceiverProxy.notifyHubOfMatchingServiceRequestFailure(sessionId);
        } catch (AttributeQueryTimeoutException exception) {
            auditAndLogTimeoutException(sessionId, attributeQueryContainerDto, exception, "Matching service attribute query has timed out, therefore not sending failure notification to saml engine.");
        }
    }

    private void logAndAuditMessageError(Exception e, AttributeQueryContainerDto attributeQueryContainerDto, SessionId sessionId) {
        String errorMessage = MatchingServiceRequestExceptionErrorMessageMapper.getErrorMessageForException(e);
        Map<EventDetailsKey, String> details = new HashMap<>();
        UUID errorId = UUID.randomUUID();
        details.put(idp_entity_id, attributeQueryContainerDto.getIssuer());
        details.put(message, errorMessage + e.getMessage());
        details.put(error_id, errorId.toString());

        EventSinkHubEvent hubEvent = new EventSinkHubEvent(
                serviceInfo,
                sessionId,
                EventSinkHubEventConstants.EventTypes.ERROR_EVENT,
                details);
        eventSinkProxy.logHubEvent(hubEvent);
        eventEmitter.record(hubEvent);
        if (attributeQueryContainerDto.isOnboarding()) {
            LOG.warn(LogFormatter.formatLog(errorId, e.getMessage()), e);
        } else {
            LOG.error(LogFormatter.formatLog(errorId, e.getMessage()), e);
        }
    }
}
