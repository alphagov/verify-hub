package uk.gov.ida.hub.samlsoapproxy.client;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlsoapproxy.logging.ExternalCommunicationEventLogger;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AttributeQueryRequestClientTest {

    private static final String SOME_MESSAGE_ID = "some-message-id";
    private static final SessionId SOME_SESSION_ID = SessionId.createNewSessionId();

    private URI matchingServiceUri;

    @Mock
    private SoapRequestClient mockSoapRequestClient;
    @Mock
    private ExternalCommunicationEventLogger externalCommunicationEventLogger;
    @Mock(answer = Answers.RETURNS_MOCKS)
    MetricRegistry metricsRegistry;
    @Mock
    private Client client;
    @Mock
    private WebTarget resource;
    @Mock
    private Invocation.Builder builder;
    @Mock
    private SoapMessageManager soapMessageManager;

    private AttributeQueryRequestClient attributeQueryRequestClientWithRealSoapRequestClient;

    private AttributeQueryRequestClient attributeQueryRequestClientWithMockSoapRequestClient;

    @Before
    public void setUp() throws Exception {
        matchingServiceUri = new URI("http://heyyeyaaeyaaaeyaeyaa.com/" + SOME_MESSAGE_ID);
        SoapRequestClient soapRequestClient = new SoapRequestClient(soapMessageManager, client);
        attributeQueryRequestClientWithRealSoapRequestClient = new AttributeQueryRequestClient(soapRequestClient, externalCommunicationEventLogger, metricsRegistry);
        attributeQueryRequestClientWithMockSoapRequestClient = new AttributeQueryRequestClient(mockSoapRequestClient, externalCommunicationEventLogger, metricsRegistry);

        when(soapMessageManager.wrapWithSoapEnvelope(any())).thenReturn(mock(Document.class));
        when(soapMessageManager.unwrapSoapMessage(ArgumentMatchers.<Document>any())).thenReturn(mock(Element.class));

        when(client.target(ArgumentMatchers.<URI>any())).thenReturn(resource);
        when(resource.request()).thenReturn(builder);
    }

    @Test
    public void sendQuery_expectingSuccessWithStatusCode200() throws IOException, SAXException, ParserConfigurationException {
        Element matchingServiceRequest = XmlUtils.convertToElement("<someElement/>");
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(builder.post(any(Entity.class))).thenReturn(response);

        final Element element = attributeQueryRequestClientWithRealSoapRequestClient.sendQuery(matchingServiceRequest, SOME_MESSAGE_ID, SOME_SESSION_ID, matchingServiceUri);

        assertThat(element).isNotNull();
    }

    @Test
    public void sendQuery_expectingFailureWithStatusCode303() {
        Response response = new TestResponse(SEE_OTHER.getStatusCode(), "http://see-other");
        String expectedMessage = MessageFormat.format("Matching Service response from {0} was status 303", matchingServiceUri);
        when(builder.post(any(Entity.class))).thenReturn(response);

        assertExceptionWithMessageAndInnerException(attributeQueryRequestClientWithRealSoapRequestClient, expectedMessage);
    }

    @Test
    public void sendQuery_expectingFailureWithStatusCode500() {
        Response response = new TestResponse(INTERNAL_SERVER_ERROR.getStatusCode(), "something bad happened");
        String expectedMessage = MessageFormat.format("Matching Service response from {0} was status 500", matchingServiceUri);
        when(builder.post(any(Entity.class))).thenReturn(response);

        assertExceptionWithMessageAndInnerException(attributeQueryRequestClientWithRealSoapRequestClient, expectedMessage);
    }

    @Test
    public void sendQuery_shouldThrowExceptionWithMatchingServiceConnectivityExceptionWhenSoapClientThrowsClientHandlerException() {
        ProcessingException expectedInnerException = mock(ProcessingException.class);
        String expectedMessage = "Request to Matching Service Failed At Http Layer";
        when(builder.post(any(Entity.class))).thenThrow(expectedInnerException);

        assertExceptionWithMessageAndInnerException(attributeQueryRequestClientWithRealSoapRequestClient, expectedMessage);
    }

    @Test
    public void sendQuery_shouldSendAuditHubEvent() throws Exception {
        Element matchingServiceRequest = XmlUtils.convertToElement("<someElement/>");
        when(mockSoapRequestClient.makeSoapRequest(eq(matchingServiceRequest), any(URI.class)))
                .thenReturn(mock(Element.class));

        attributeQueryRequestClientWithMockSoapRequestClient.sendQuery(matchingServiceRequest, SOME_MESSAGE_ID, SOME_SESSION_ID, matchingServiceUri);

        verify(externalCommunicationEventLogger).logMatchingServiceRequest(SOME_MESSAGE_ID, SOME_SESSION_ID, matchingServiceUri);
    }

    private void assertExceptionWithMessageAndInnerException(
        AttributeQueryRequestClient attributeQueryRequestClient,
        String expectedMessage) {

        try {
            attributeQueryRequestClient.sendQuery(mock(Element.class), SOME_MESSAGE_ID, SOME_SESSION_ID, matchingServiceUri);
            fail("Expected exception not thrown");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(AttributeQueryRequestClient.MatchingServiceException.class);
            assertThat(e.getMessage()).isEqualTo(expectedMessage);
        }
    }
}
