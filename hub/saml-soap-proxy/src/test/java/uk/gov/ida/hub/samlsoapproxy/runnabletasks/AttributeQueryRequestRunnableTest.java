package uk.gov.ida.hub.samlsoapproxy.runnabletasks;

import com.codahale.metrics.Counter;
import org.glassfish.jersey.internal.util.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.event.Level;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.common.shared.security.verification.exceptions.CertificateChainValidationException;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEvent;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.samlsoapproxy.domain.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlsoapproxy.domain.TimeoutEvaluator;
import uk.gov.ida.hub.samlsoapproxy.exceptions.AttributeQueryTimeoutException;
import uk.gov.ida.hub.samlsoapproxy.exceptions.InvalidSamlRequestInAttributeQueryException;
import uk.gov.ida.hub.samlsoapproxy.proxy.HubMatchingServiceResponseReceiverProxy;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.eventsink.EventDetailsKey.message;
import static uk.gov.ida.hub.samlsoapproxy.builders.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;
import static uk.gov.ida.saml.core.test.builders.AttributeQueryBuilder.anAttributeQuery;

@RunWith(OpenSAMLMockitoRunner.class)
public class AttributeQueryRequestRunnableTest {

    private final ServiceInfoConfiguration serviceInfoConfiguration = new ServiceInfoConfiguration("a service name");
    @Mock
    private ExecuteAttributeQueryRequest executeAttributeQueryRequest;
    @Mock
    private Counter counter;
    @Mock
    private TimeoutEvaluator timeoutEvaluator;
    @Mock
    private EventSinkProxy eventSinkProxy;
    @Mock
    private EventEmitter eventEmitter;
    @Mock
    private HubMatchingServiceResponseReceiverProxy hubMatchingServiceResponseReceiverProxy;

    private AttributeQueryRequestRunnable attributeQueryRequestRunnable;
    private AttributeQueryContainerDto attributeQueryContainerDto;
    private URI matchingServiceUri = URI.create("/another-uri");
    private SessionId sessionId = SessionId.createNewSessionId();

    @Before
    public void setup() {
        attributeQueryContainerDto = anAttributeQueryContainerDto(anAttributeQuery().build())
                .withMatchingServiceUri(matchingServiceUri)
                .build();
        attributeQueryRequestRunnable = new AttributeQueryRequestRunnable(
                sessionId,
                attributeQueryContainerDto,
                executeAttributeQueryRequest,
                counter,
                timeoutEvaluator,
                hubMatchingServiceResponseReceiverProxy,
                serviceInfoConfiguration,
                eventSinkProxy,
                eventEmitter);
    }

    @Test
    public void shouldIncrementAndDecrementCounter() {
        verify(counter).inc();
        attributeQueryRequestRunnable.run();
        verify(counter).dec();
    }

    @Test
    public void run_shouldEvaluateTimeoutBeforeSendingRequest() {
        doThrow(new AttributeQueryTimeoutException()).when(timeoutEvaluator).hasAttributeQueryTimedOut(attributeQueryContainerDto);

        //This represents the queue being full/slow - so don't make matters worse by doing slow work that's not needed.
        attributeQueryRequestRunnable.run();

        verify(executeAttributeQueryRequest, never()).execute(any(SessionId.class), any(AttributeQueryContainerDto.class));
        verify(eventSinkProxy, times(1)).logHubEvent(isA(EventSinkHubEvent.class));
        verify(eventEmitter, times(1)).record(isA(EventSinkHubEvent.class));
    }

    @Test
    public void run_shouldLogToAudit_ButShouldNotNotifySamlEngine_RequestHasTimedOut_AndWhenMessageFromSamlEngineValidationFailsWithUnexpectedException() throws Exception {
        when(executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto))
                .thenThrow(new InvalidSamlRequestInAttributeQueryException("Attribute Query had invalid XML.",new RuntimeException("test exception")));

        doNothing().doThrow(new AttributeQueryTimeoutException()).when(timeoutEvaluator).hasAttributeQueryTimedOut(attributeQueryContainerDto);

        attributeQueryRequestRunnable.run();

        verify(eventSinkProxy, times(2)).logHubEvent(isA(EventSinkHubEvent.class)); //One for the timeout, one for the message error
        verify(eventEmitter, times(2)).record(isA(EventSinkHubEvent.class));
        verify(hubMatchingServiceResponseReceiverProxy, never()).notifyHubOfMatchingServiceRequestFailure(sessionId);
    }

    @Test
    public void run_shouldNotSendResponse_IfAttributeQueryHasTimedOut_AfterSendingMessage() throws Exception {
        final Element matchingServiceResponse = XmlUtils.convertToElement("<someResponse/>");
        when(executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto))
                .thenReturn(matchingServiceResponse);
        doNothing().doThrow(new AttributeQueryTimeoutException()).when(timeoutEvaluator).hasAttributeQueryTimedOut(attributeQueryContainerDto);

        attributeQueryRequestRunnable.run();

        verify(executeAttributeQueryRequest).execute(sessionId, attributeQueryContainerDto);
        verify(hubMatchingServiceResponseReceiverProxy, never()).notifyHubOfAResponseFromMatchingService(
                any(SessionId.class),
                any(String.class)
        );
        verify(eventSinkProxy, times(1)).logHubEvent(isA(EventSinkHubEvent.class));
        verify(eventEmitter, times(1)).record(isA(EventSinkHubEvent.class));
    }

    @Test
    public void run_shouldSendResponse() throws Exception {
        final Element matchingServiceResponse = XmlUtils.convertToElement("<someResponse/>");

        when(executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto))
                .thenReturn(matchingServiceResponse);

        attributeQueryRequestRunnable.run();

        String stringifiedResponse = XmlUtils.writeToString(matchingServiceResponse);
        verify(hubMatchingServiceResponseReceiverProxy).notifyHubOfAResponseFromMatchingService(
                eq(sessionId),
                eq(Base64.encodeAsString(stringifiedResponse))
        );
        verify(timeoutEvaluator, times(2)).hasAttributeQueryTimedOut(attributeQueryContainerDto);
    }

    @Test
    public void run_shouldSayIfTimeoutWasBeforeSendingMessage() throws Exception {
        final Element matchingServiceResponse = XmlUtils.convertToElement("<someResponse/>");
        when(executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto))
                .thenReturn(matchingServiceResponse);

        doThrow(new AttributeQueryTimeoutException()).when(timeoutEvaluator).hasAttributeQueryTimedOut(attributeQueryContainerDto);

        attributeQueryRequestRunnable.run();

        final ArgumentCaptor<EventSinkHubEvent> loggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        final ArgumentCaptor<EventSinkHubEvent> emitterLoggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(loggedHubEvent.capture());
        verify(eventEmitter).record(emitterLoggedHubEvent.capture());
        assertThat(loggedHubEvent.getValue().getDetails().get(EventDetailsKey.message)).isEqualTo("Matching service attribute timed out before even being sent.");
        assertThat(emitterLoggedHubEvent.getValue().getDetails().get(EventDetailsKey.message)).isEqualTo("Matching service attribute timed out before even being sent.");
    }

    @Test
    public void run_shouldLogToAuditAndReturnToSamlEngineWhenInboundMessageValidationFailsWithUnexpectedException() throws Exception {
        when(executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto))
                .thenThrow(new RuntimeException("Uh-oh, something unexpected happened!"));

        attributeQueryRequestRunnable.run();

        final ArgumentCaptor<EventSinkHubEvent> loggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        final ArgumentCaptor<EventSinkHubEvent> emitterLoggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(loggedHubEvent.capture());
        verify(eventEmitter).record(emitterLoggedHubEvent.capture());
        assertThat(loggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        assertThat(emitterLoggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        verify(hubMatchingServiceResponseReceiverProxy).notifyHubOfMatchingServiceRequestFailure(sessionId);
        verify(timeoutEvaluator, times(2)).hasAttributeQueryTimedOut(attributeQueryContainerDto); //Request has not timed out - didn't throw.
    }

    @Test
    public void run_shouldNotifySamlEngineAndLogErrorWhenMatchingServiceResponseIsNotProperlySigned() {
        when(executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto))
                .thenThrow(new SamlTransformationErrorException("Signature was not valid", Level.ERROR));

        attributeQueryRequestRunnable.run();

        final ArgumentCaptor<EventSinkHubEvent> loggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        final ArgumentCaptor<EventSinkHubEvent> emitterLoggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(loggedHubEvent.capture());
        verify(eventEmitter).record(emitterLoggedHubEvent.capture());
        assertThat(loggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        assertThat(emitterLoggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        verify(hubMatchingServiceResponseReceiverProxy).notifyHubOfMatchingServiceRequestFailure(sessionId);
        verify(timeoutEvaluator, times(2)).hasAttributeQueryTimedOut(attributeQueryContainerDto);
        assertThat(loggedHubEvent.getValue().getDetails().get(message)).doesNotContain("Incorrect message provided by caller");
        assertThat(emitterLoggedHubEvent.getValue().getDetails().get(message)).doesNotContain("Incorrect message provided by caller");

    }

    @Test
    public void run_shouldNotifySamlEngineAndLogErrorWhenMatchingServiceRequestIsNotProperlySigned() {
        when(executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto))
                .thenThrow(new InvalidSamlRequestInAttributeQueryException("Attribute Query had invalid Saml", new Exception()));

        attributeQueryRequestRunnable.run();

        final ArgumentCaptor<EventSinkHubEvent> loggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        final ArgumentCaptor<EventSinkHubEvent> emitterLoggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(loggedHubEvent.capture());
        verify(eventEmitter).record(emitterLoggedHubEvent.capture());
        assertThat(loggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        assertThat(emitterLoggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        verify(hubMatchingServiceResponseReceiverProxy).notifyHubOfMatchingServiceRequestFailure(sessionId);
        verify(timeoutEvaluator, times(2)).hasAttributeQueryTimedOut(attributeQueryContainerDto);
        assertThat(loggedHubEvent.getValue().getDetails().get(message)).contains("Incorrect message provided by caller");
        assertThat(emitterLoggedHubEvent.getValue().getDetails().get(message)).contains("Incorrect message provided by caller");
    }

    @Test
    public void run_shouldNotifySamlEngineAndLogErrorWhenACertificateCannotBeChainedToThoseInTheTrustStore() throws IOException, SAXException, ParserConfigurationException {
        when(executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto))
                .thenThrow(new CertificateChainValidationException("cert chain validation error", new Exception()));

        attributeQueryRequestRunnable.run();

        final ArgumentCaptor<EventSinkHubEvent> loggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        final ArgumentCaptor<EventSinkHubEvent> emitterLoggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(loggedHubEvent.capture());
        verify(eventEmitter).record(emitterLoggedHubEvent.capture());
        assertThat(loggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        assertThat(emitterLoggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        verify(hubMatchingServiceResponseReceiverProxy, times(1)).notifyHubOfMatchingServiceRequestFailure(sessionId);
        verify(timeoutEvaluator, times(2)).hasAttributeQueryTimedOut(attributeQueryContainerDto);
        assertThat(loggedHubEvent.getValue().getDetails().get(message)).contains("Problem with the matching service's signing certificate");
        assertThat(emitterLoggedHubEvent.getValue().getDetails().get(message)).contains("Problem with the matching service's signing certificate");

    }

    @Test
    public void run__shouldNotNotifySamlEngineWhenAttributeQueryHasTimedOutBeforeBeingSentToMSA() {
        doThrow(new AttributeQueryTimeoutException("Attribute Query timed out by 1 seconds."))
                .when(timeoutEvaluator).hasAttributeQueryTimedOut(attributeQueryContainerDto);

        attributeQueryRequestRunnable.run();

        final ArgumentCaptor<EventSinkHubEvent> loggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        final ArgumentCaptor<EventSinkHubEvent> emitterLoggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(loggedHubEvent.capture());
        verify(eventEmitter).record(emitterLoggedHubEvent.capture());
        assertThat(loggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        assertThat(emitterLoggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        verify(hubMatchingServiceResponseReceiverProxy, times(0)).notifyHubOfMatchingServiceRequestFailure(sessionId);
        verify(timeoutEvaluator, times(1)).hasAttributeQueryTimedOut(attributeQueryContainerDto);
        assertThat(loggedHubEvent.getValue().getDetails().get(message)).contains("Matching service attribute timed out before even being sent.");
        assertThat(emitterLoggedHubEvent.getValue().getDetails().get(message)).contains("Matching service attribute timed out before even being sent.");
    }

    @Test
    public void run_shouldNotNotifySamlEngineWhenMSAResponseIsReceivedAfterAttributeQueryHasTimedOut() throws IOException, SAXException, ParserConfigurationException {
        final Element matchingServiceResponse = XmlUtils.convertToElement("<someResponse/>");
        when(executeAttributeQueryRequest.execute(sessionId, attributeQueryContainerDto))
                .thenReturn(matchingServiceResponse);

        // this stubbing does nothing the first time it is called, and throws an exception the second time it is called
        doNothing().doThrow(new AttributeQueryTimeoutException("Attribute Query timed out by 1 seconds.")).when(timeoutEvaluator).hasAttributeQueryTimedOut(attributeQueryContainerDto);

        attributeQueryRequestRunnable.run();

        final ArgumentCaptor<EventSinkHubEvent> loggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        final ArgumentCaptor<EventSinkHubEvent> emitterLoggedHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(loggedHubEvent.capture());
        verify(eventEmitter).record(emitterLoggedHubEvent.capture());
        assertThat(loggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        assertThat(emitterLoggedHubEvent.getValue().getSessionId()).isEqualTo(sessionId.toString());
        verify(hubMatchingServiceResponseReceiverProxy, times(0)).notifyHubOfMatchingServiceRequestFailure(sessionId);
        verify(timeoutEvaluator, times(2)).hasAttributeQueryTimedOut(attributeQueryContainerDto);
        assertThat(loggedHubEvent.getValue().getDetails().get(message)).contains("Matching service attribute query has timed out, therefore not sending failure notification to saml engine.");
        assertThat(emitterLoggedHubEvent.getValue().getDetails().get(message)).contains("Matching service attribute query has timed out, therefore not sending failure notification to saml engine.");
    }
}
