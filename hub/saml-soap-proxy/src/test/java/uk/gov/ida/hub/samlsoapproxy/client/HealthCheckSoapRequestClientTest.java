package uk.gov.ida.hub.samlsoapproxy.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckSoapRequestClientTest {

    @Mock
    private SoapMessageManager soapMessageManager;
    @Mock
    private Client client;
    @Mock
    private WebTarget webResource;
    @Mock
    private Invocation.Builder webResourceBuilder;
    @Mock
    private Response response;

    private HealthCheckSoapRequestClient healthCheckSoapRequestClient;

    @Before
    public void setUp() {
        when(client.target(any(URI.class))).thenReturn(webResource);
        when(webResource.request()).thenReturn(webResourceBuilder);
        when(webResourceBuilder.post(any(Entity.class))).thenReturn(response);
        doNothing().when(response).close();
        healthCheckSoapRequestClient = new HealthCheckSoapRequestClient(soapMessageManager, client);
    }

    @Test(expected = ApplicationException.class)
    public void makeSoapRequestForHealthCheck_shouldThrowWhenResponseNot200() throws URISyntaxException {
        when(response.getStatus()).thenReturn(502);
        URI matchingServiceUri = new URI("http://heyyeyaaeyaaaeyaeyaa.com/abc1");

        healthCheckSoapRequestClient.makeSoapRequestForHealthCheck(null, matchingServiceUri);
    }

    @Test
    public void makePost_shouldEnsureResponseInputStreamIsClosedWhenResponseCodeIsNot200() throws URISyntaxException {
        when(response.getStatus()).thenReturn(502);
        URI matchingServiceUri = new URI("http://heyyeyaaeyaaaeyaeyaa.com/abc1");

        try {
            healthCheckSoapRequestClient.makeSoapRequestForHealthCheck(null, matchingServiceUri);
            fail("Exception should have been thrown");
        } catch (ApplicationException e) {
            verify(response).close();
        }
    }
}
