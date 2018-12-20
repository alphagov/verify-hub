package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractContextExceptionMapperTest {

    @Mock
    private HttpServletRequest servletRequest;

    private TestExceptionMapper mapper;

    @Before
    public void setUp() {
        Provider<HttpServletRequest> provider = () -> servletRequest;
        mapper = new TestExceptionMapper(provider);
    }

    @Test
    public void shouldDelegateToExceptionMapperForServiceNameRootWithoutSessionId() {
        assertDelegateExceptionMapperIsUsedForNoContextUri(Urls.SharedUrls.SERVICE_NAME_ROOT);
    }
    
    @Test
    public void shouldDelegateToExceptionMapperForSaml2SsoReceiverApiRootWithoutSessionId() {
        assertDelegateExceptionMapperIsUsedForNoContextUri(Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);
    }

    @Test
    public void shouldDelegateToExceptionMapperForSaml2SsoSenderApiRootWithoutSessionId() {
        assertDelegateExceptionMapperIsUsedForNoContextUri(Urls.SamlProxyUrls.SAML2_SSO_SENDER_API_ROOT);
    }

    @Test
    public void shouldDelegateToExceptionMapperWhenSessionIdIsQueryStringPath() {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(SessionId.createNewSessionId().getSessionId());

        String expectedMessage = "Expected message";
        Response response = mapper.toResponse(new RuntimeException(expectedMessage));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(expectedMessage);
    }

    @Test
    public void shouldReturnInternalServerErrorWhenThereIsNoSessionIdAndTheRequestUriIsNotAKnownNoContextPath() {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("");
        when(servletRequest.getParameter(Urls.SharedUrls.RELAY_STATE_PARAM)).thenReturn("");
        String unknownUri = UUID.randomUUID().toString();
        when(servletRequest.getRequestURI()).thenReturn(unknownUri);

        Response response = mapper.toResponse(new RuntimeException("We don't expect to see this message"));

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldReturnResponseStatusOfNotFoundWhenANotFoundExceptionIsThrown() {
        Response response = mapper.toResponse(new NotFoundException());

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    private void assertDelegateExceptionMapperIsUsedForNoContextUri(final String noContextUri) {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("");
        when(servletRequest.getParameter(Urls.SharedUrls.RELAY_STATE_PARAM)).thenReturn("");
        when(servletRequest.getRequestURI()).thenReturn(noContextUri);

        String expectedMessage = "Expected message";
        Response response = mapper.toResponse(new RuntimeException(expectedMessage));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(expectedMessage);
    }

    private class TestExceptionMapper extends AbstractContextExceptionMapper<RuntimeException> {
        private TestExceptionMapper(Provider<HttpServletRequest> context) {
            super(context);
        }

        @Override
        protected Response handleException(RuntimeException e) {
            return Response.ok().entity(e.getMessage()).build();
        }
    }
}
